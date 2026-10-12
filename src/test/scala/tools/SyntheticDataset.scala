package tools

import java.io.{File, PrintWriter, Serializable}
import java.util
import org.geotools.api.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.geotools.api.data.SimpleFeatureStore
import org.geotools.data.DefaultTransaction
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.{Coordinate, GeometryFactory, MultiPolygon, Polygon}
import ucar.ma2.{DataType, Array => NcArray}
import ucar.nc2.Attribute
import ucar.nc2.write.NetcdfFormatWriter

/**
  * Generates a small, deterministic, synthetic input dataset for Zissou:
  *
  *  - monthly NetCDF-3 u/v/w files laid out the way FlowFileIterator expects
  *    (`<flow>/{u,v,w}/ocean_<var>_<year>_<month>.nc`, OFAM-style names),
  *  - a reef polygon shapefile with HABITAT / PATCH_NUM attributes,
  *  - YAML configs: a quick smoke run, a larger benchmark run, and an edge run
  *    whose larvae drift off the southern edge of the grid.
  *
  * Note the flow period must start no earlier than the first release date:
  * the simulation loop stops immediately when nothing is spawning or pelagic.
  *
  * The same inputs always produce the same files, so the dataset can be
  * regenerated at any time and needs no version control.
  *
  * Run with: sbt "Test/runMain tools.SyntheticDataset [outputDir]"
  */
object SyntheticDataset {

  // FlowFileIterator hard-codes these depth levels (and reads the first 15)
  val Depths: Array[Double] = Array(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2,
    41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0)

  val MinLon = 153.0
  val MaxLon = 156.0
  val MinLat = -33.0
  val MaxLat = -25.0
  val Step = 0.1
  val Year = 2002
  val Months = Seq(1, 2, 3)
  val FillValue = -1.0e20f

  val Lons: Array[Double] = axis(MinLon, MaxLon)
  val Lats: Array[Double] = axis(MinLat, MaxLat)

  // (patch number, habitat, lon, lat)
  val Patches: Seq[(Int, String, Double, Double)] = Seq(
    (101, "Reef", 153.7, -28.8),
    (102, "Reef", 153.9, -28.5),
    (103, "Other", 153.8, -28.2),
    (104, "Reef", 154.0, -27.9),
    (105, "Reef", 153.6, -27.6),
    (106, "Rocky intertidal", 153.9, -27.3),
    (107, "Reef", 153.5, -28.0),
    (108, "Reef", 153.7, -27.1)
  )

  def main(args: Array[String]): Unit = {
    val root = new File(if (args.nonEmpty) args(0) else "data/synthetic")
    for (variable <- Seq("u", "v", "w")) {
      new File(root, s"flow/$variable").mkdirs()
    }
    new File(root, "habitat").mkdirs()
    new File(root, "output").mkdirs()

    for (month <- Months) writeFlowMonth(root, month)
    writeHabitat(new File(root, "habitat/reefs.shp"))
    val reefSites = Seq(Patches(0), Patches(3), Patches(6)) // patches 101, 104, 107
    writeConfig(root, "smoke", reefSites, larvaePerSite = 100)
    writeConfig(root, "bench", reefSites, larvaePerSite = 20000)
    // released right at the southern edge of the grid, so larvae drift off it
    val edgeSites = Seq(
      (101, "Reef", 154.0, MinLat + 0.3),
      (104, "Reef", 154.2, MinLat + 0.5)
    )
    writeConfig(root, "edge", edgeSites, larvaePerSite = 200)
    println(s"Synthetic dataset written to ${root.getAbsolutePath}")
  }

  private def axis(from: Double, to: Double): Array[Double] =
    Array.tabulate(math.round((to - from) / Step).toInt + 1)(
      i => BigDecimal(from + i * Step).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
    )

  private def daysInMonth(month: Int): Int =
    java.time.YearMonth.of(Year, month).lengthOfMonth

  // --- the synthetic ocean --------------------------------------------------

  /** Coastline: land lies west of this longitude. */
  private def coast(lat: Double): Double =
    153.35 + 0.08 * math.sin(2 * math.Pi * (lat - MinLat))

  /** Sea floor depth in metres; deepens quickly away from the coast. */
  private def bottom(lon: Double, lat: Double): Double =
    15.0 + 400.0 * (lon - coast(lat))

  /** (u, v, w) in m/s at a point on a given day, or None for land / sea floor. */
  private def velocity(
      lon: Double,
      lat: Double,
      depth: Double,
      day: Int
  ): Option[(Float, Float, Float)] = {
    if (lon < coast(lat) || depth > bottom(lon, lat)) None
    else {
      val shear = math.exp(-depth / 80.0)
      // a southward boundary current with a pulsing core, like a western boundary jet
      val jet = -0.35 * math.exp(-math.pow((lon - 154.2) / 0.35, 2)) *
        (1 + 0.2 * math.sin(2 * math.Pi * day / 20.0))
      // a weak northward counter-flow inshore so some larvae are retained
      val inshore = 0.05 * math.exp(-math.pow((lon - 153.5) / 0.15, 2))
      val v = (jet + inshore) * shear
      // eddy-like cross-shore flow plus a small onshore drift
      val u = (0.08 * math.sin(2 * math.Pi * (lat - MinLat) / 1.0 + 2 * math.Pi * day / 12.0) - 0.03) * shear
      val w = 1.0e-5 * math.sin(2 * math.Pi * (lon - MinLon) + day)
      Some((u.toFloat, v.toFloat, w.toFloat))
    }
  }

  // --- NetCDF ---------------------------------------------------------------

  private def writeFlowMonth(root: File, month: Int): Unit = {
    val nt = daysInMonth(month)
    for ((name, component) <- Seq(("u", 0), ("v", 1), ("w", 2))) {
      val file = new File(root, f"flow/$name/ocean_${name}_$Year%d_$month%02d.nc")
      writeVariable(file, name, nt, month, component)
    }
  }

  private def writeVariable(
      file: File,
      name: String,
      nt: Int,
      month: Int,
      component: Int
  ): Unit = {
    val nz = Depths.length
    val ny = Lats.length
    val nx = Lons.length
    val builder = NetcdfFormatWriter.createNewNetcdf3(file.getPath)
    builder.addDimension("Time", nt)
    builder.addDimension("st_ocean", nz)
    builder.addDimension("yu_ocean", ny)
    builder.addDimension("xu_ocean", nx)

    val epoch = s"days since $Year-${"%02d".format(month)}-01 00:00:00"
    builder
      .addVariable("Time", DataType.DOUBLE, "Time")
      .addAttribute(new Attribute("units", epoch))
      .addAttribute(new Attribute("calendar", "gregorian"))
      .addAttribute(new Attribute("axis", "T"))
    builder
      .addVariable("st_ocean", DataType.DOUBLE, "st_ocean")
      .addAttribute(new Attribute("units", "m"))
      .addAttribute(new Attribute("positive", "down"))
      .addAttribute(new Attribute("axis", "Z"))
    builder
      .addVariable("yu_ocean", DataType.DOUBLE, "yu_ocean")
      .addAttribute(new Attribute("units", "degrees_north"))
      .addAttribute(new Attribute("axis", "Y"))
    builder
      .addVariable("xu_ocean", DataType.DOUBLE, "xu_ocean")
      .addAttribute(new Attribute("units", "degrees_east"))
      .addAttribute(new Attribute("axis", "X"))
    builder
      .addVariable(name, DataType.FLOAT, "Time st_ocean yu_ocean xu_ocean")
      .addAttribute(new Attribute("units", "m/s"))
      .addAttribute(new Attribute("_FillValue", FillValue))
      .addAttribute(new Attribute("missing_value", FillValue))

    val writer = builder.build()
    try {
      // noon of each day, like OFAM's daily means
      val times = Array.tabulate(nt)(_ + 0.5)
      writer.write("Time", NcArray.factory(DataType.DOUBLE, Array(nt), times))
      writer.write("st_ocean", NcArray.factory(DataType.DOUBLE, Array(nz), Depths))
      writer.write("yu_ocean", NcArray.factory(DataType.DOUBLE, Array(ny), Lats))
      writer.write("xu_ocean", NcArray.factory(DataType.DOUBLE, Array(nx), Lons))

      val data = new Array[Float](nt * nz * ny * nx)
      var i = 0
      for (t <- 0 until nt; z <- 0 until nz; y <- 0 until ny; x <- 0 until nx) {
        data(i) = velocity(Lons(x), Lats(y), Depths(z), t) match {
          case Some(v) =>
            component match {
              case 0 => v._1
              case 1 => v._2
              case _ => v._3
            }
          case None => FillValue
        }
        i += 1
      }
      writer.write(name, NcArray.factory(DataType.FLOAT, Array(nt, nz, ny, nx), data))
    } finally {
      writer.close()
    }
  }

  // --- shapefile ------------------------------------------------------------

  private def writeHabitat(file: File): Unit = {
    val factory = new GeometryFactory()
    val half = 0.03

    val builder = new SimpleFeatureTypeBuilder()
    builder.setName("reefs")
    builder.setCRS(DefaultGeographicCRS.WGS84)
    // attribute order matters: SimpleFeatureAdaptor reads the geometry by index
    builder.add("the_geom", classOf[MultiPolygon])
    builder.add("HABITAT", classOf[String])
    builder.add("STATE", classOf[String])
    builder.add("GROUPID", classOf[java.lang.Integer])
    builder.add("PATCH_NUM", classOf[java.lang.Integer])
    builder.add("X_COORD", classOf[java.lang.Double])
    builder.add("Y_COORD", classOf[java.lang.Double])
    builder.add("AREA", classOf[java.lang.Double])
    builder.add("PERIMETER", classOf[java.lang.Double])
    val featureType: SimpleFeatureType = builder.buildFeatureType()

    val features = new util.ArrayList[SimpleFeature]
    for ((patch, habitat, lon, lat) <- Patches) {
      val ring = factory.createLinearRing(
        Array(
          new Coordinate(lon - half, lat - half),
          new Coordinate(lon - half, lat + half),
          new Coordinate(lon + half, lat + half),
          new Coordinate(lon + half, lat - half),
          new Coordinate(lon - half, lat - half)
        )
      )
      val polygon: Polygon = factory.createPolygon(ring)
      val multi = factory.createMultiPolygon(Array(polygon))
      val feature = new SimpleFeatureBuilder(featureType)
      feature.add(multi)
      feature.add(habitat)
      feature.add("QLD")
      feature.add(Int.box(patch / 100))
      feature.add(Int.box(patch))
      feature.add(Double.box(lon))
      feature.add(Double.box(lat))
      feature.add(Double.box(polygon.getArea))
      feature.add(Double.box(polygon.getLength))
      features.add(feature.buildFeature(null))
    }

    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    params.put("create spatial index", java.lang.Boolean.TRUE)
    val store = new ShapefileDataStoreFactory().createNewDataStore(params)
    store.createSchema(featureType)
    val transaction = new DefaultTransaction("create")
    try {
      val source = store.getFeatureSource(store.getTypeNames.head)
      val featureStore = source.asInstanceOf[SimpleFeatureStore]
      featureStore.setTransaction(transaction)
      featureStore.addFeatures(new ListFeatureCollection(featureType, features))
      transaction.commit()
    } catch {
      case e: Throwable => transaction.rollback(); throw e
    } finally {
      transaction.close()
      store.dispose()
    }
  }

  // --- config ---------------------------------------------------------------

  private def writeConfig(
      root: File,
      name: String,
      sites: Seq[(Int, String, Double, Double)],
      larvaePerSite: Int
  ): Unit = {
    val spawning = sites
      .map {
        case (patch, _, lon, lat) =>
          s"""      -
         name: patch $patch
         patchNumber: $patch
         site:
          longitude: $lon
          latitude: $lat
          depth: 5
         numberOfLarvae: $larvaePerSite
         releasePeriod:
          start: $Year-01-05
          end: $Year-01-20
         interval: 5"""
      }
      .mkString("\n")

    val yaml =
      s"""settings:
  randomSeed: 1234
spawn:
  spawningLocation:
$spawning
turbulence:
  horizontalDiffusionCoefficient: 300
  verticalDiffusionCoefficient: 15
  applyTurbulence: true
  interval: 1
larva:
  species: fish
  isMortal: true
  mortalityRate: 0.26
  ontogeny:
   hatching: 0
   preflexion: 2
   flexion: 5
   postflexion: 8
  swimming:
   strategy: one
   ability: directed
   criticalSwimmingSpeed: 0.463
   inSituSwimmingPotential: 0.25
   endurance: 0.5
   reynoldsEffect: false
   hatchSwimmingSpeed: 1
  pelagicLarvalDuration:
   mean: 15
   stdev: 1.5
   pldType: Fixed
   nonSettlementPeriod: 5
  dielProbabilities:
   depths: [25, 50, 75, 100]
   day: [0.1, 0.3, 0.4, 0.2]
   night: [0.3, 0.3, 0.3, 0.1]
  ovmProbabilities:
   implementation: Stage
   depths: [5, 50, 100]
   hatching: [0, 0, 0]
   preflexion: [0.4, 0.5, 0.1]
   flexion: [0.35, 0.5, 0.15]
   postflexion: [0.05, 0.85, 0.1]
flow:
  netcdfFilePath: ${new File(root, "flow").getPath}
  period:
   start: $Year-01-05
   end: $Year-02-20
  timeStep:
   unit: Hour
   duration: 2
  includeVerticalVelocity: true
habitat:
  shapeFilePath: ${new File(root, "habitat/reefs.shp").getPath}
  buffer:
   settlement: 5
   olfactory: 10
output:
  includeLarvaeHistory: false
  saveOutputFilePath: ${new File(root, "output").getPath}
  percentage: 5
  prefix: $name
  logLevel: info
  logFile: ${new File(root, s"output/$name.log").getPath}
"""
    val out = new PrintWriter(new File(root, s"$name.yaml"))
    try out.write(yaml) finally out.close()
  }
}

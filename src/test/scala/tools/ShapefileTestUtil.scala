package tools

import java.io.{File, Serializable}
import java.util
import org.geotools.api.data.SimpleFeatureStore
import org.geotools.data.DefaultTransaction
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.{Coordinate, GeometryFactory, LineString, MultiPolygon}

/** Writes small shapefiles for tests. */
object ShapefileTestUtil {

  private val factory = new GeometryFactory()

  /** A line shapefile; a None entry is a record with no geometry, as GeoTools wrote when broken. */
  def writeLines(file: File, lines: Seq[Option[Seq[(Double, Double)]]]): Unit = {
    val builder = new SimpleFeatureTypeBuilder()
    builder.setName("larva")
    builder.setCRS(DefaultGeographicCRS.WGS84)
    builder.add("the_geom", classOf[LineString])
    val featureType = builder.buildFeatureType()
    val features = new util.ArrayList[org.geotools.api.feature.simple.SimpleFeature]
    lines.foreach { line =>
      val feature = new SimpleFeatureBuilder(featureType)
      feature.add(line.map(points => factory.createLineString(points.map { case (x, y) => new Coordinate(x, y) }.toArray)).orNull)
      features.add(feature.buildFeature(null))
    }
    write(file, featureType, features)
  }

  /** The geometry of a patch made of one or more pieces, each given by its corners. */
  def multiPolygon(pieces: Seq[Seq[(Double, Double)]]): MultiPolygon =
    factory.createMultiPolygon(pieces.map { corners =>
      factory.createPolygon(factory.createLinearRing((corners :+ corners.head).map { case (x, y) => new Coordinate(x, y) }.toArray))
    }.toArray)

  /** A polygon shapefile with the PATCH_NUM and HABITAT attributes the model reads. */
  def writeReefs(file: File, reefs: Seq[(Int, String, Seq[(Double, Double)])]): Unit =
    writeReefParts(file, reefs.map { case (patch, habitat, corners) => (patch, habitat, Seq(corners)) })

  /** As writeReefs, with each patch made of one or more separate pieces. */
  def writeReefParts(file: File, reefs: Seq[(Int, String, Seq[Seq[(Double, Double)]])]): Unit = {
    val builder = new SimpleFeatureTypeBuilder()
    builder.setName("reefs")
    builder.setCRS(DefaultGeographicCRS.WGS84)
    builder.add("the_geom", classOf[MultiPolygon])
    builder.add("HABITAT", classOf[String])
    builder.add("PATCH_NUM", classOf[java.lang.Integer])
    val featureType = builder.buildFeatureType()
    val features = new util.ArrayList[org.geotools.api.feature.simple.SimpleFeature]
    reefs.foreach { case (patch, habitat, pieces) =>
      val feature = new SimpleFeatureBuilder(featureType)
      feature.add(multiPolygon(pieces))
      feature.add(habitat)
      feature.add(Int.box(patch))
      features.add(feature.buildFeature(null))
    }
    write(file, featureType, features)
  }

  private def write(file: File, featureType: org.geotools.api.feature.simple.SimpleFeatureType, features: util.ArrayList[org.geotools.api.feature.simple.SimpleFeature]): Unit = {
    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    val store = new ShapefileDataStoreFactory().createNewDataStore(params)
    store.createSchema(featureType)
    val transaction = new DefaultTransaction("create")
    try {
      val source = store.getFeatureSource(store.getTypeNames.head).asInstanceOf[SimpleFeatureStore]
      source.setTransaction(transaction)
      source.addFeatures(new ListFeatureCollection(featureType, features))
      transaction.commit()
    } finally {
      transaction.close()
      store.dispose()
    }
  }
}

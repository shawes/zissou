package tools

import java.io.File
import java.time.{LocalDate, YearMonth}
import ucar.ma2.{DataType, Array => NcArray}
import ucar.nc2.Attribute
import ucar.nc2.write.NetcdfFormatWriter

/**
  * Monthly flow files laid out like BRAN2020's: u and v on the corners of the cells
  * (xu_ocean, yu_ocean) and the centres of the layers (st_ocean), w on the centres of the cells
  * (xt_ocean, yt_ocean) and the bottoms of the layers (sw_ocean), half a cell and half a level
  * from them.
  *
  * Every field is a plane in longitude, latitude and depth, so the flow between nodes, and at
  * the level nearest a depth, is known exactly. u and v are at the corners 153.0, 153.1 ... 154.0
  * east and 28.5 ... 27.5 south, w half a cell (0.05) north-east of them, so w's nodes begin at
  * 153.05 and 28.45, and the layers of u and v are centred on 2.5, 7.5, 12.5 ... and w is at the
  * bottoms of the same layers, 5, 10, 15 ...
  */
object StaggeredFlowDataset {

  val Lons: Array[Double] = GriddedFlowDataset.axis(153.0, 154.0, 0.1)
  val Lats: Array[Double] = GriddedFlowDataset.axis(-28.5, -27.5, 0.1)
  val WLons: Array[Double] = Lons.map(x => BigDecimal(x + 0.05).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
  val WLats: Array[Double] = Lats.map(y => BigDecimal(y + 0.05).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
  val Levels: Array[Double] = Array.tabulate(15)(k => 2.5 + 5.0 * k)
  val Interfaces: Array[Double] = Array.tabulate(15)(k => 5.0 * (k + 1))

  // the flow in m/s: a plane in longitude (degrees east of 153), latitude (degrees north of 28.5 south) and depth (m)
  def u(lon: Double, lat: Double, depth: Double): Double = 0.10 + 0.20 * (lon - 153) + 0.05 * (lat + 28.5) + 1e-4 * depth
  def v(lon: Double, lat: Double, depth: Double): Double = -0.10 + 0.04 * (lon - 153) - 0.15 * (lat + 28.5) + 2e-4 * depth
  def w(lon: Double, lat: Double, depth: Double): Double = 1e-3 + 4e-4 * (lon - 153) + 7e-4 * (lat + 28.5) + 1e-5 * depth

  /** What a curvature adds to every field: a quadratic in longitude and latitude, which bicubic interpolation reproduces and bilinear does not. */
  def curve(lon: Double, lat: Double, curvature: Double): Double = curvature * (math.pow(lon - 153, 2) + math.pow(lat + 28.5, 2))

  def write(root: File, months: Seq[(Int, Int)], curvature: Double = 0.0): Unit =
    for (variable <- Seq("u", "v", "w"); (year, month) <- months) {
      val directory = new File(root, variable)
      directory.mkdirs()
      val (x, y, z, lons, lats, levels) =
        if (variable == "w") ("xt_ocean", "yt_ocean", "sw_ocean", WLons, WLats, Interfaces)
        else ("xu_ocean", "yu_ocean", "st_ocean", Lons, Lats, Levels)
      val plane: (Double, Double, Double) => Double = variable match { case "u" => u; case "v" => v; case _ => w }
      val field: (Double, Double, Double) => Double = (lon, lat, depth) => plane(lon, lat, depth) + curve(lon, lat, curvature)
      writeMonth(new File(directory, f"ocean_${variable}_$year%d_$month%02d.nc"), variable, year, month, (x, y, z), lons, lats, levels, field)
    }

  private def writeMonth(
      file: File, name: String, year: Int, month: Int, names: (String, String, String),
      lons: Array[Double], lats: Array[Double], levels: Array[Double], field: (Double, Double, Double) => Double
  ): Unit = {
    val (xName, yName, zName) = names
    val days = YearMonth.of(year, month).lengthOfMonth
    val (nz, ny, nx) = (levels.length, lats.length, lons.length)
    val builder = NetcdfFormatWriter.createNewNetcdf3(file.getPath)
    builder.addDimension("Time", days)
    builder.addDimension(zName, nz)
    builder.addDimension(yName, ny)
    builder.addDimension(xName, nx)
    builder.addVariable("Time", DataType.DOUBLE, "Time")
      .addAttribute(new Attribute("units", f"days since $year%d-$month%02d-01 00:00:00"))
      .addAttribute(new Attribute("calendar", "gregorian")).addAttribute(new Attribute("axis", "T"))
    builder.addVariable(zName, DataType.DOUBLE, zName)
      .addAttribute(new Attribute("units", "m")).addAttribute(new Attribute("positive", "down")).addAttribute(new Attribute("axis", "Z"))
    builder.addVariable(yName, DataType.DOUBLE, yName)
      .addAttribute(new Attribute("units", "degrees_north")).addAttribute(new Attribute("axis", "Y"))
    builder.addVariable(xName, DataType.DOUBLE, xName)
      .addAttribute(new Attribute("units", "degrees_east")).addAttribute(new Attribute("axis", "X"))
    builder.addVariable(name, DataType.FLOAT, s"Time $zName $yName $xName")
      .addAttribute(new Attribute("units", "m/s")).addAttribute(new Attribute("_FillValue", -1.0e20f)).addAttribute(new Attribute("missing_value", -1.0e20f))
    val writer = builder.build()
    try {
      writer.write("Time", NcArray.factory(DataType.DOUBLE, Array(days), Array.tabulate(days)(_ + 0.5)))
      writer.write(zName, NcArray.factory(DataType.DOUBLE, Array(nz), levels))
      writer.write(yName, NcArray.factory(DataType.DOUBLE, Array(ny), lats))
      writer.write(xName, NcArray.factory(DataType.DOUBLE, Array(nx), lons))
      val data = new Array[Float](days * nz * ny * nx)
      var i = 0
      for (_ <- 0 until days; z <- 0 until nz; y <- 0 until ny; x <- 0 until nx) {
        data(i) = field(lons(x), lats(y), levels(z)).toFloat
        i += 1
      }
      writer.write(name, NcArray.factory(DataType.FLOAT, Array(days, nz, ny, nx), data))
    } finally writer.close()
  }
}

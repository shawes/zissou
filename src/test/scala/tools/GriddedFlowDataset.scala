package tools

import java.io.File
import java.time.{LocalDate, YearMonth}
import ucar.ma2.{DataType, Array => NcArray}
import ucar.nc2.Attribute
import ucar.nc2.write.NetcdfFormatWriter

/**
  * Monthly NetCDF-3 flow files, one u, v and w file per month, laid out the way FlowFileIterator
  * reads them, for any grid and any field. The 15 depth levels it expects are written, every
  * level with the same field, and the daily fields are stamped at noon, like the daily means
  * of an ocean model.
  */
object GriddedFlowDataset {

  private val Depths = Array(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0)

  /** A regularly spaced axis from `from` to `to`, to the thousandth of a degree. */
  def axis(from: Double, to: Double, step: Double): Array[Double] =
    Array.tabulate(math.round((to - from) / step).toInt + 1)(
      i => BigDecimal(from + i * step).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
    )

  /**
    * Writes the files under root/u, root/v and root/w.
    *
    * @param value the flow, in m/s, of a variable ("u", "v" or "w") at a longitude and latitude
    *              on a day, the day being its number counted from 2002-01-01
    */
  def write(root: File, months: Seq[(Int, Int)], lons: Array[Double], lats: Array[Double], packedScale: Option[Double] = None)(
      value: (String, Double, Double, Long) => Double
  ): Unit =
    for (variable <- Seq("u", "v", "w"); (year, month) <- months) {
      val directory = new File(root, variable)
      directory.mkdirs()
      // like BRAN2020, u and v can be packed in 16 bits with a scale factor and w left as it is
      val scale = packedScale.filter(_ => variable != "w")
      writeMonth(new File(directory, f"ocean_${variable}_$year%d_$month%02d.nc"), variable, year, month, lons, lats, value, scale)
    }

  private def writeMonth(
      file: File,
      name: String,
      year: Int,
      month: Int,
      lons: Array[Double],
      lats: Array[Double],
      value: (String, Double, Double, Long) => Double,
      packedScale: Option[Double]
  ): Unit = {
    val days = YearMonth.of(year, month).lengthOfMonth
    val (nz, ny, nx) = (Depths.length, lats.length, lons.length)
    val builder = NetcdfFormatWriter.createNewNetcdf3(file.getPath)
    builder.addDimension("Time", days)
    builder.addDimension("st_ocean", nz)
    builder.addDimension("yu_ocean", ny)
    builder.addDimension("xu_ocean", nx)
    builder.addVariable("Time", DataType.DOUBLE, "Time")
      .addAttribute(new Attribute("units", f"days since $year%d-$month%02d-01 00:00:00"))
      .addAttribute(new Attribute("calendar", "gregorian")).addAttribute(new Attribute("axis", "T"))
    builder.addVariable("st_ocean", DataType.DOUBLE, "st_ocean")
      .addAttribute(new Attribute("units", "m")).addAttribute(new Attribute("positive", "down")).addAttribute(new Attribute("axis", "Z"))
    builder.addVariable("yu_ocean", DataType.DOUBLE, "yu_ocean")
      .addAttribute(new Attribute("units", "degrees_north")).addAttribute(new Attribute("axis", "Y"))
    builder.addVariable("xu_ocean", DataType.DOUBLE, "xu_ocean")
      .addAttribute(new Attribute("units", "degrees_east")).addAttribute(new Attribute("axis", "X"))
    packedScale match {
      case Some(scale) =>
        builder.addVariable(name, DataType.SHORT, "Time st_ocean yu_ocean xu_ocean")
          .addAttribute(new Attribute("units", "m/s")).addAttribute(new Attribute("scale_factor", scale.toFloat))
          .addAttribute(new Attribute("add_offset", 0.0f)).addAttribute(new Attribute("_FillValue", (-32768).toShort))
          .addAttribute(new Attribute("missing_value", (-32768).toShort))
      case None =>
        builder.addVariable(name, DataType.FLOAT, "Time st_ocean yu_ocean xu_ocean")
          .addAttribute(new Attribute("units", "m/s")).addAttribute(new Attribute("_FillValue", -1.0e20f)).addAttribute(new Attribute("missing_value", -1.0e20f))
    }

    val writer = builder.build()
    try {
      writer.write("Time", NcArray.factory(DataType.DOUBLE, Array(days), Array.tabulate(days)(_ + 0.5)))
      writer.write("st_ocean", NcArray.factory(DataType.DOUBLE, Array(nz), Depths))
      writer.write("yu_ocean", NcArray.factory(DataType.DOUBLE, Array(ny), lats))
      writer.write("xu_ocean", NcArray.factory(DataType.DOUBLE, Array(nx), lons))
      def flow(t: Int, y: Int, x: Int): Double =
        value(name, lons(x), lats(y), java.time.temporal.ChronoUnit.DAYS.between(UniformFlowDataset.Epoch, LocalDate.of(year, month, 1).plusDays(t)))
      packedScale match {
        case Some(scale) =>
          val data = new Array[Short](days * nz * ny * nx)
          var i = 0
          for (t <- 0 until days; _ <- 0 until nz; y <- 0 until ny; x <- 0 until nx) {
            data(i) = math.round(flow(t, y, x) / scale).toShort
            i += 1
          }
          writer.write(name, NcArray.factory(DataType.SHORT, Array(days, nz, ny, nx), data))
        case None =>
          val data = new Array[Float](days * nz * ny * nx)
          var i = 0
          for (t <- 0 until days; _ <- 0 until nz; y <- 0 until ny; x <- 0 until nx) {
            data(i) = flow(t, y, x).toFloat
            i += 1
          }
          writer.write(name, NcArray.factory(DataType.FLOAT, Array(days, nz, ny, nx), data))
      }
    } finally writer.close()
  }
}

package tools

import java.io.File
import java.time.{LocalDate, YearMonth}
import ucar.ma2.{DataType, Array => NcArray}
import ucar.nc2.Attribute
import ucar.nc2.write.NetcdfFormatWriter

/**
  * Monthly NetCDF flow files in which every field is the same everywhere and equal to a
  * multiple of the day it belongs to, counted from 2002-01-01. Reading a velocity back
  * therefore says exactly which day's field it came from, and a blend of two days is
  * visible as a day number with a fraction.
  *
  *   u = 0.01 * day,  v = -0.005 * day,  w = 0.000001 * day
  *
  * The grid lies inside the area FlowFileIterator reads, with its 15 depth levels, and the
  * daily fields are stamped at noon, like the daily means of an ocean model.
  */
object UniformFlowDataset {

  val Epoch: LocalDate = LocalDate.of(2002, 1, 1)
  val U = 0.01
  val V = -0.005
  val W = 0.000001

  private val Depths = Array(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0)
  private val Lons = Array.tabulate(11)(i => BigDecimal(153.0 + i * 0.1).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
  private val Lats = Array.tabulate(11)(i => BigDecimal(-28.5 + i * 0.1).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)

  /** The day number a date has in these files. */
  def dayNumber(date: LocalDate): Long = java.time.temporal.ChronoUnit.DAYS.between(Epoch, date)

  def dayNumber(year: Int, month: Int, day: Int): Long = dayNumber(LocalDate.of(year, month, day))

  /** Writes one file per (year, month) under root/u, root/v and root/w. */
  def write(root: File, months: Seq[(Int, Int)]): Unit =
    for ((variable, scale) <- Seq(("u", U), ("v", V), ("w", W)); (year, month) <- months) {
      val directory = new File(root, variable)
      directory.mkdirs()
      writeMonth(new File(directory, f"ocean_${variable}_$year%d_$month%02d.nc"), variable, scale, year, month)
    }

  private def writeMonth(file: File, name: String, scale: Double, year: Int, month: Int): Unit = {
    val days = YearMonth.of(year, month).lengthOfMonth
    val (nz, ny, nx) = (Depths.length, Lats.length, Lons.length)
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
    builder.addVariable(name, DataType.FLOAT, "Time st_ocean yu_ocean xu_ocean")
      .addAttribute(new Attribute("units", "m/s")).addAttribute(new Attribute("_FillValue", -1.0e20f)).addAttribute(new Attribute("missing_value", -1.0e20f))

    val writer = builder.build()
    try {
      writer.write("Time", NcArray.factory(DataType.DOUBLE, Array(days), Array.tabulate(days)(_ + 0.5)))
      writer.write("st_ocean", NcArray.factory(DataType.DOUBLE, Array(nz), Depths))
      writer.write("yu_ocean", NcArray.factory(DataType.DOUBLE, Array(ny), Lats))
      writer.write("xu_ocean", NcArray.factory(DataType.DOUBLE, Array(nx), Lons))
      val data = new Array[Float](days * nz * ny * nx)
      for (t <- 0 until days; i <- 0 until nz * ny * nx) {
        data(t * nz * ny * nx + i) = (scale * dayNumber(LocalDate.of(year, month, 1).plusDays(t))).toFloat
      }
      writer.write(name, NcArray.factory(DataType.FLOAT, Array(days, nz, ny, nx), data))
    } finally writer.close()
  }
}

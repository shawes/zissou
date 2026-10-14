package io

import java.nio.file.{Files, Path, StandardCopyOption}
import java.time.YearMonth
import grizzled.slf4j.Logging
import physical.flow.FlowRegion
import ucar.ma2.{DataType, Range, Section}
import ucar.nc2.{Attribute, Variable}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.write.NetcdfFormatWriter
import ucar.unidata.geoloc.{LatLonPoint, LatLonRect}
import scala.jdk.CollectionConverters.*

/**
  * Copies the part of a flow source that a run reads, its region and its top levels, into a folder of monthly files
  * laid out as `LocalFlowSource` reads them: `<target>/<variable>/<prefix>_<variable>_<year>_<month>.nc`. A run on the
  * copy reads the same values as one on the source, from the disk instead of the network.
  *
  * Values are copied as they are stored, so BRAN2020's 16 bit packed integers stay packed, with the scale factor and
  * offset that say what they mean. Each file is written under another name and renamed when it is complete, so a copy
  * that is stopped can be started again and goes on from the first file it does not have.
  */
class FlowCache(source: FlowSource, variables: List[String], region: FlowRegion, depthLevels: Int, prefix: String) extends Logging {

  private val bounds = new LatLonRect(LatLonPoint.create(region.south, region.west), LatLonPoint.create(region.north, region.east))

  /** The file a variable's month is copied to. */
  def fileOf(target: Path, variable: String, month: YearMonth): Path =
    target.resolve(variable).resolve(f"${prefix}_${variable}_${month.getYear}%d_${month.getMonthValue}%02d.nc")

  /** What a month of a variable comes to: the days, levels, rows and columns copied, and the bytes they take. */
  case class Part(variable: String, month: YearMonth, days: Int, levels: Int, rows: Int, columns: Int, bytesPerValue: Int) {
    def bytes: Long = days.toLong * levels * rows * columns * bytesPerValue
  }

  /** The size of what would be copied, read from the source's description of its files: nothing else is read. */
  def plan(months: Seq[YearMonth]): Seq[Part] =
    for (month <- months; variable <- variables) yield {
      val dataset = source.open(variable, month)
      try {
        val (grid, raw, ranges) = select(dataset, variable)
        Part(variable, month, grid.getCoordinateSystem.getTimeAxis1D.getSize.toInt, ranges(1).length, ranges(2).length,
          ranges(3).length, raw.getDataType.getSize)
      } finally dataset.close()
    }

  /** Copies the months to the target folder, skipping the files it already has, and returns the files written. */
  def copy(months: Seq[YearMonth], target: Path): Seq[Path] =
    for {
      month <- months
      variable <- variables
      file = fileOf(target, variable, month)
      if !Files.exists(file)
    } yield {
      Files.createDirectories(file.getParent)
      val partial = file.resolveSibling(file.getFileName.toString + ".part")
      Files.deleteIfExists(partial)
      val dataset = source.open(variable, month)
      try write(dataset, variable, month, partial)
      finally dataset.close()
      Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      info(s"Copied $variable for $month to $file (${Files.size(file) / 1000000} MB)")
      file
    }

  // The grid of a variable, its values as stored (not scaled), and the ranges of time, depth, latitude and longitude
  // to copy, in the order of its dimensions
  private def select(dataset: ucar.nc2.dt.grid.GridDataset, variable: String) = {
    val grid = Option(dataset.findGridByName(variable)).getOrElse(
      throw new IllegalArgumentException(s"The flow data has no variable $variable")
    )
    val coordinates = grid.getCoordinateSystem
    val levels = Option(coordinates.getVerticalAxis).map(_.getSize.toInt).getOrElse(
      throw new IllegalStateException(s"$variable has no depth axis")
    )
    if (levels < depthLevels) throw new IllegalArgumentException(s"$variable has $levels depth levels, fewer than the $depthLevels asked for")
    // A few nodes more than the region on every side: which nodes a reader takes for a region depends on whether they
    // are at the edge of the file, where it guesses where their cells end, so a copy that ended at the region's own
    // nodes would give a run on it fewer nodes than one on the original.
    val horizontal = coordinates.getRangesFromLatLonRect(bounds)
    def widened(range: Range, size: Long) =
      new Range(math.max(0, range.first - FlowCache.Margin), math.min(size.toInt - 1, range.last + FlowCache.Margin))
    val ranges = List(
      new Range(0, coordinates.getTimeAxis1D.getSize.toInt - 1),
      new Range(0, depthLevels - 1),
      widened(horizontal.get(0), coordinates.getYHorizAxis.getSize),
      widened(horizontal.get(1), coordinates.getXHorizAxis.getSize)
    )
    // the values as they are stored: the grid's own variable scales packed values, and they are kept packed
    val enhanced = grid.getVariable
    val raw: Variable = enhanced match {
      case ds: ucar.nc2.dataset.VariableDS => Option(ds.getOriginalVariable).getOrElse(ds)
      case other => other
    }
    (grid, raw, ranges)
  }

  private def write(dataset: ucar.nc2.dt.grid.GridDataset, variable: String, month: YearMonth, file: Path): Unit = {
    val (grid, raw, ranges) = select(dataset, variable)
    val coordinates: GridCoordSystem = grid.getCoordinateSystem
    val axes = List(coordinates.getTimeAxis1D, coordinates.getVerticalAxis, coordinates.getYHorizAxis, coordinates.getXHorizAxis)
    val names = axes.map(_.getShortName)
    val sizes = ranges.map(_.length)

    val builder = NetcdfFormatWriter.createNewNetcdf3(file.toString).setFill(false)
    names.zip(sizes).foreach { case (name, size) => builder.addDimension(name, size) }
    axes.zip(names).foreach { case (axis, name) =>
      val added = builder.addVariable(name, DataType.DOUBLE, name)
      kept(axis.attributes().asScala).foreach(added.addAttribute)
    }
    val data = builder.addVariable(variable, raw.getDataType, names.mkString(" "))
    kept(raw.attributes().asScala).foreach(data.addAttribute)

    val writer = builder.build()
    try {
      axes.zip(names).zip(ranges).foreach { case ((axis, name), range) =>
        val values = axis.read(new Section(java.util.Collections.singletonList(range)))
        writer.write(name, values.reshape(Array(range.length)))
      }
      // a day at a time, which keeps each request to the server small, and a failed one is tried again
      for (day <- 0 until sizes.head) {
        val section = new Section((new Range(day, day) :: ranges.tail).asJava)
        val values = source.retrying(s"reading $variable for day ${day + 1} of $month")(raw.read(section))
        writer.write(variable, Array(day, 0, 0, 0), values.reshape(Array(1) ++ sizes.tail))
      }
    } finally writer.close()
  }

  // The attributes worth copying: what the values mean (units, scale factor, offset, the value for none) and not the
  // ones a reader adds or that only the source's format has, which begin with an underscore
  private def kept(attributes: Iterable[Attribute]): Iterable[Attribute] =
    attributes.filter(a => !a.getShortName.startsWith("_") || a.getShortName == "_FillValue")
}

object FlowCache extends Logging {

  /** The nodes copied beyond the region on each side. */
  val Margin = 3

  private val Usage =
    """Usage: zissou cache-flow CONFIG TARGET [--start YYYY-MM-DD] [--end YYYY-MM-DD] [--size]
      |  Copies the flow a configuration reads (its server or folder, region and depth levels, and the temperature if
      |  it needs it) into TARGET, one folder for each variable, for the months of its flow period or of the period
      |  given. A copy that was stopped goes on where it left off. With --size it only says how big the copy would be.
      |""".stripMargin

  /** The months a period touches, from the one it starts in to the one it ends in. */
  def monthsOf(start: java.time.LocalDate, end: java.time.LocalDate): Seq[YearMonth] =
    Iterator.iterate(YearMonth.from(start))(_.plusMonths(1)).takeWhile(!_.isAfter(YearMonth.from(end))).toSeq

  def main(args: Array[String]): Unit = {
    val (options, positional) = {
      val named = scala.collection.mutable.Map.empty[String, String]
      val rest = scala.collection.mutable.ListBuffer.empty[String]
      val it = args.iterator
      while (it.hasNext) it.next() match {
        case "--size" => named("size") = "true"
        case flag if flag.startsWith("--") && it.hasNext => named(flag.drop(2)) = it.next()
        case other => rest += other
      }
      (named.toMap, rest.toList)
    }
    if (positional.length != 2 || (options.keySet -- Set("size", "start", "end")).nonEmpty) {
      Console.err.print(Usage)
      sys.exit(2)
    }
    val config = new io.config.ConfigurationFile().read(positional(0))
    val target = Path.of(positional(1))
    import io.config.ConfigMappings._
    val flow: physical.flow.Flow = config.flow
    flow.includeTemperature = config.larva.temperatureMortality.isDefined
    val start = options.get("start").map(java.time.LocalDate.parse).getOrElse(java.time.LocalDate.parse(config.flow.period.start.take(10)))
    val end = options.get("end").map(java.time.LocalDate.parse).getOrElse(java.time.LocalDate.parse(config.flow.period.end.take(10)))
    val months = monthsOf(start, end)
    val variables = FlowFileIterator.variablesFor(flow)
    val cache = new FlowCache(FlowFileIterator.sourceFor(flow), variables, flow.region, flow.depthLevels, flow.filePrefix)

    val parts = cache.plan(months)
    val missing = parts.filterNot(p => Files.exists(cache.fileOf(target, p.variable, p.month)))
    val first = parts.head
    println(f"${variables.mkString(", ")} for ${months.head} to ${months.last}: ${first.rows} x ${first.columns} points, " +
      f"${first.levels} levels, ${first.bytesPerValue} bytes a value")
    println(f"  ${parts.map(_.bytes).sum / 1e6}%.0f MB in all, ${missing.map(_.bytes).sum / 1e6}%.0f MB of it not yet in $target")
    if (!options.contains("size")) {
      val written = cache.copy(months, target)
      println(s"  Wrote ${written.size} files.")
    }
  }
}

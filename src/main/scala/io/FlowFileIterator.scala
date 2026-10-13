package io

import java.time.YearMonth
import scala.collection.mutable.{ListBuffer, Queue}
import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.{GridDataset}
import ucar.unidata.geoloc.{LatLonPoint, LatLonRect}

class FlowFileIterator(val flow: Flow, source: FlowSource) extends Logging {

  /** Reads the flow from the folder the flow is configured with. */
  def this(flow: Flow) = this(flow, new LocalFlowSource(flow.netcdfFilePath, FlowFileIterator.Variables))

  val variables = FlowFileIterator.Variables
  var dataset: List[(GridDataset, String)] = List.empty
  val grids =
    new Queue[(List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)]
  // the depth of each level read, from the surface, as the data's own depth axis gives them;
  // set once the first file is open
  var depths: List[Double] = List.empty
  // the month of the file read last; declared before initialFlowData runs, which reads a file
  private var loadedMonth: Option[YearMonth] = None
  val netcdfHandler = new NetcdfFileHandler()
  val latLonBounds = new LatLonRect(
    LatLonPoint.create(flow.region.south, flow.region.west),
    LatLonPoint.create(flow.region.north, flow.region.east)
  )
  val depthRange: Range = new Range(0, flow.depthLevels - 1)

  initialFlowData()

  private def initialFlowData(): Unit = {
    val start = flow.period.getStart.toLocalDate
    val startMonth = YearMonth.of(start.getYear, start.getMonthOfYear)
    if (!source.has(startMonth)) {
      throw new IllegalArgumentException(s"There is no flow file for $startMonth in ${source.description}")
    }
    readMonth(startMonth)
    depths = readDepths()
    val startDay: Int = flow.period.getStart.toLocalDate.dayOfMonth.get
    grids.enqueue((readDay(startDay, latLonBounds, depthRange), startDay, getDaysInMonth))
    ensureFollowingDayExists()
    loadFollowingDay()
  }

  // The depths of the levels read: the first of the depth axis, which fails with a message
  // if the data has fewer levels than are asked for, or none
  private def readDepths(): List[Double] = {
    val (grids, name) = dataset.head
    val axis = grids.findGridByName(name).getCoordinateSystem.getVerticalAxis
    if (axis == null) {
      throw new IllegalStateException(s"The flow data has no depth axis, so the depth of its levels is not known")
    }
    if (axis.getSize < flow.depthLevels) {
      throw new IllegalArgumentException(
        s"The flow data has ${axis.getSize} depth levels, fewer than the ${flow.depthLevels} that flow.depthLevels asks for"
      )
    }
    List.tabulate(flow.depthLevels)(level => axis.getCoordValue(level))
  }

  /** The two daily fields held now: the day the run starts on and the day after. */
  def current: FlowGridWrapper =
    new FlowGridWrapper(
      depths,
      grids.map(grid => grid._1.toList).toList,
      stampOf(grids.front),
      stampOf(grids.last)
    )

  /** Moves on a day: the later field becomes the earlier, and the day after it is read. */
  def next(): FlowGridWrapper = {
    // checked first, so that running out of data leaves the window as it was
    ensureFollowingDayExists()
    clearYesterday()
    loadFollowingDay()
    current
  }

  // The time a day's field is for, read from the time axis of the data (noon, for daily means)
  private def stampOf(
      day: (List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)
  ): LocalDateTime = {
    val axis = day._1.head._2.getTimeAxis1D
    if (axis == null) {
      throw new IllegalStateException("The flow data has no time axis, so the time of each day's field is not known")
    }
    new LocalDateTime(axis.getCalendarDate(0).getMillis, DateTimeZone.UTC)
  }

  /** Whether there is a file that has not been read yet, for a month after the one read last. */
  def hasNext: Boolean = loadedMonth.exists(month => source.firstAfter(month).isDefined)

  def closeAllOpenDatasets(): Unit = {
    if (dataset.nonEmpty) {
      dataset.foreach(dataset => dataset._1.close())
    }
  }

  private def clearYesterday(): Unit = {
    if (grids.size == 2) {
      grids.dequeue()
    }
  }

  def shutdown(): Unit = {
    closeAllOpenDatasets()
    netcdfHandler.shutdown()
  }

  private def readDay(
      dayOfMonth: Int,
      latLonBounds: LatLonRect,
      depthRange: Range
  ): List[(Array[Array[Array[Float]]], GridCoordSystem)] = {

    val day = dataset
      .map(
        dataset =>
          dataset._1
            .findGridByName(dataset._2)
            .subset(
              new Range(dayOfMonth - 1, dayOfMonth - 1),
              depthRange,
              latLonBounds,
              0,
              0,
              0
            )
      )
      .toList

    day.map(
      grid =>
        (
          grid
            .readDataSlice(0, -1, -1, -1)
            .copyToNDJavaArray()
            .asInstanceOf[Array[Array[Array[Float]]]],
          grid.getCoordinateSystem()
        )
    )

  }

  // Reads the files of a month: the one the run starts in, or the one after the month read last
  private def readMonth(month: YearMonth): Unit = {
    loadedMonth.foreach { previous =>
      if (month != previous.plusMonths(1) || !source.has(month)) {
        val later = source.firstAfter(previous).map(_.toString).getOrElse("nothing")
        throw new IllegalStateException(
          s"The flow files go from $previous to $later: there is no file for ${previous.plusMonths(1)}."
        )
      }
    }
    val nextDayData = ListBuffer.empty[(GridDataset, String)]
    variables.foreach(
      variable => nextDayData += ((source.open(variable, month), variable))
    )
    closeAllOpenDatasets()
    dataset = nextDayData.toList
    loadedMonth = Some(month)
  }

  // the day after the newest one held is in this month's file, or in the next month's
  private def ensureFollowingDayExists(): Unit = {
    if (isLastDayOfMonth(grids.last) && !hasNext) {
      throw new FlowDataExhaustedException(
        s"The flow files hold nothing after ${stampOf(grids.last).toLocalDate}."
      )
    }
  }

  private def loadFollowingDay(): Unit = {
    val newest = grids.last
    val startsNextMonth = isLastDayOfMonth(newest)
    if (startsNextMonth) readMonth(loadedMonth.get.plusMonths(1))
    val day = if (startsNextMonth) 1 else newest._2 + 1
    grids.enqueue((readDay(day, latLonBounds, depthRange), day, getDaysInMonth))
  }

  private def isLastDayOfMonth(
      day: (List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)
  ): Boolean = day._2 == day._3

  private def getDaysInMonth: Int =
    dataset.head._1.getCalendarDateEnd.getDayOfMonth()

}

object FlowFileIterator {
  val Variables = List("u", "v", "w") //TODO: Put this into the config file
}

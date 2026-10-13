package io

import java.time.YearMonth
import java.util.concurrent.{ExecutionException, Executors, Future, ThreadFactory, TimeUnit}
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

  /** Reads the flow from the folder, or the OPeNDAP server, the flow is configured with. */
  def this(flow: Flow) = this(flow, FlowFileIterator.sourceFor(flow))

  private type Day = (List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)

  val variables = FlowFileIterator.Variables
  var dataset: List[(GridDataset, String)] = List.empty
  val grids = new Queue[Day]
  // the depth of each level read, from the surface, as the data's own depth axis gives them;
  // set once the first file is open
  var depths: List[Double] = List.empty
  // the month of the file read last; declared before initialFlowData runs, which reads a file
  @volatile private var loadedMonth: Option[YearMonth] = None
  val netcdfHandler = new NetcdfFileHandler()
  val latLonBounds = new LatLonRect(
    LatLonPoint.create(flow.region.south, flow.region.west),
    LatLonPoint.create(flow.region.north, flow.region.east)
  )
  val depthRange: Range = new Range(0, flow.depthLevels - 1)

  // From a source that is slow to read, the day after the newest one held is read while the
  // larvae are moved, by one thread of its own, so that the run waits only for what is not
  // ready. It reads the datasets, which only it uses while it is reading.
  private val reader =
    if (source.isRemote) Some(Executors.newSingleThreadExecutor(new ThreadFactory {
      def newThread(task: Runnable): Thread = {
        val thread = new Thread(task, "zissou-flow-reader")
        thread.setDaemon(true)
        thread
      }
    }))
    else None
  private var ahead: Option[Future[Day]] = None

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
  private def stampOf(day: Day): LocalDateTime = {
    val axis = day._1.head._2.getTimeAxis1D
    if (axis == null) {
      throw new IllegalStateException("The flow data has no time axis, so the time of each day's field is not known")
    }
    new LocalDateTime(axis.getCalendarDate(0).getMillis, DateTimeZone.UTC)
  }

  /**
    * Whether the files hold a month after the newest day held. That is not the month read last:
    * a source that reads ahead may have opened the next month's file already.
    */
  def hasNext: Boolean = {
    val newest = stampOf(grids.last).toLocalDate
    source.firstAfter(YearMonth.of(newest.getYear, newest.getMonthOfYear)).isDefined
  }

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
    ahead.foreach(_.cancel(true))
    ahead = None
    reader.foreach { reader =>
      reader.shutdownNow()
      reader.awaitTermination(10, TimeUnit.SECONDS)
    }
    closeAllOpenDatasets()
    netcdfHandler.shutdown()
  }

  private def readDay(
      dayOfMonth: Int,
      latLonBounds: LatLonRect,
      depthRange: Range
  ): List[(Array[Array[Array[Float]]], GridCoordSystem)] = source.retrying(s"reading day $dayOfMonth of ${loadedMonth.getOrElse("the first month")}") {

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
    val day = ahead match {
      case Some(future) =>
        ahead = None
        try future.get()
        catch { case e: ExecutionException => throw e.getCause }
      case None => fetchFollowingDay(grids.last)
    }
    grids.enqueue(day)
    readAhead()
  }

  // The day after one: read from its month's file, or from the next month's if it is the last
  private def fetchFollowingDay(newest: Day): Day = {
    val startsNextMonth = isLastDayOfMonth(newest)
    if (startsNextMonth) readMonth(loadedMonth.get.plusMonths(1))
    val day = if (startsNextMonth) 1 else newest._2 + 1
    (readDay(day, latLonBounds, depthRange), day, getDaysInMonth)
  }

  // Starts reading the day after the newest one held, unless the files end there
  private def readAhead(): Unit =
    reader.foreach { reader =>
      val newest = grids.last
      if (!(isLastDayOfMonth(newest) && !hasNext)) {
        ahead = Some(reader.submit(new java.util.concurrent.Callable[Day] { def call(): Day = fetchFollowingDay(newest) }))
      }
    }

  private def isLastDayOfMonth(day: Day): Boolean = day._2 == day._3

  private def getDaysInMonth: Int =
    dataset.head._1.getCalendarDateEnd.getDayOfMonth()

}

object FlowFileIterator {
  val Variables = List("u", "v", "w") //TODO: Put this into the config file

  /** The folder, or the OPeNDAP server, that a flow is configured to read. */
  def sourceFor(flow: Flow): FlowSource =
    if (OpenDapFlowSource.isUrl(flow.netcdfFilePath)) new OpenDapFlowSource(flow.netcdfFilePath, flow.filePrefix)
    else new LocalFlowSource(flow.netcdfFilePath, Variables)
}

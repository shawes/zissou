package io

import java.io.File
import scala.collection.mutable.{ListBuffer, Queue}
import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.{GridDataset}
import ucar.unidata.geoloc.{LatLonPoint, LatLonRect}

class FlowFileIterator(val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  val variables = List("u", "v", "w") //TODO: Put this into the config file
  var dataset: List[(GridDataset, String)] = List.empty
  val grids =
    new Queue[(List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)]
  val depths = List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7,
    65.7, 75.2, 85.0, 95.0, 105.0) //TODO: Get this info from the netcdf file
  // the month of the file read last; declared before initialFlowData runs, which reads a file
  private var loadedMonth: Option[java.time.YearMonth] = None
  val netcdfHandler = new NetcdfFileHandler()
  val getFileInformation = initialiseFiles()
  var currentFile: Int = getFileInformation._2
  val files = getFileInformation._1
  val latLonBounds = new LatLonRect(
    LatLonPoint.create(-50.0, 142.0),
    LatLonPoint.create(-10.0, 180.0)
  )
  val depthRange: Range = new Range(0, 14)

  initialFlowData()

  private def initialFlowData(): Unit = {
    readMonth()
    val startDay: Int = flow.period.getStart.toLocalDate.dayOfMonth.get
    grids.enqueue((readDay(startDay, latLonBounds, depthRange), startDay, getDaysInMonth))
    ensureFollowingDayExists()
    loadFollowingDay()
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

  /** Whether there is a file that has not been read yet: currentFile is the one after the last read. */
  def hasNext: Boolean = currentFile < files.size

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

  private def readMonth(): Unit = {
    val month = monthOf(files(currentFile))
    loadedMonth.foreach { previous =>
      if (month != previous.plusMonths(1)) {
        throw new IllegalStateException(
          s"The flow files go from $previous to $month: there is no file for ${previous.plusMonths(1)}."
        )
      }
    }
    val nextDayData = ListBuffer.empty[(GridDataset, String)]
    variables.foreach(
      variable => nextDayData += ((loadNextFlowFile(variable), variable))
    )
    closeAllOpenDatasets()
    dataset = nextDayData.toList
    loadedMonth = Some(month)
    currentFile += 1
  }

  private def monthOf(filename: String): java.time.YearMonth = {
    val date = getDateFromFileName(filename)
    java.time.YearMonth.of(date.getYear, date.getMonthOfYear)
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
    if (startsNextMonth) readMonth()
    val day = if (startsNextMonth) 1 else newest._2 + 1
    grids.enqueue((readDay(day, latLonBounds, depthRange), day, getDaysInMonth))
  }

  private def isLastDayOfMonth(
      day: (List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)
  ): Boolean = day._2 == day._3

  private def loadNextFlowFile(prefix: String): GridDataset = {
    val path = flow.netcdfFilePath + "/" + prefix
    val files =
      new File(path).list().filter(p => p.endsWith(NetcdfExtension)).sorted
    val filename = path + "/" + files(currentFile)
    netcdfHandler.openLocalFile(filename)
  }

  private def initialiseFiles(): (Array[String], Int) = {
    val path = flow.netcdfFilePath + "/" + "u"
    val files =
      new File(path).list().filter(p => p.endsWith(NetcdfExtension)).sorted
    files.foreach(f => info(s"$f"))
    val startMonth = files.indexWhere(
      file =>
        isFileForDate(
          flow.period.getStart.toLocalDate,
          getDateFromFileName(file)
        )
    )
    if (startMonth < 0) {
      val start = flow.period.getStart.toLocalDate
      throw new IllegalArgumentException(
        f"There is no flow file for ${start.getYear}%d-${start.getMonthOfYear}%02d in $path"
      )
    }
    (files, startMonth)
  }

  private def getDateFromFileName(filename: String): LocalDate = {
    debug(s"Getting date from file name $filename")
    val prefix = filename.split('.')
    val sections = prefix(0).split('_')
    new LocalDateTime(sections(2).toInt, sections(3).toInt, 1, 1, 1).toLocalDate
  }

  private def isFileForDate(
      startDate: LocalDate,
      fileDate: LocalDate
  ): Boolean = {
    startDate.year == fileDate.year && startDate.monthOfYear.get == fileDate.monthOfYear.get
  }

  private def getDaysInMonth: Int =
    dataset.head._1.getCalendarDateEnd.getDayOfMonth()

}

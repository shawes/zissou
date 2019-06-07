package io

import java.io.File
import scala.collection.mutable.{ListBuffer, Queue}
import com.github.nscala_time.time.Imports._
import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.{GridDataset}
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}

class FlowFileIterator(val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  val variables = List("u", "v", "w") //TODO: Put this into the config file
  var dataset: List[(GridDataset, String)] = List.empty
  val grids =
    new Queue[(List[(Array[Array[Array[Float]]], GridCoordSystem)], Int, Int)]
  val depths = List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7,
    65.7, 75.2, 85.0, 95.0, 105.0) //TODO: Get this info from the netcdf file
  val netcdfHandler = new NetcdfFileHandler()
  val getFileInformation = initialiseFiles()
  var currentFile: Int = getFileInformation._2
  val files = getFileInformation._1
  val latLonBounds = new LatLonRect(
    new LatLonPointImpl(-50.0, 142.0),
    new LatLonPointImpl(-10.0, 180.0)
  )
  val depthRange: Range = new Range(0, 14)

  initialFlowData()

  private def initialFlowData(): Unit = {
    readMonth()
    val startDay: Int = flow.period.getStart.toLocalDate.dayOfMonth.get
    val day1 = readDay(startDay, latLonBounds, depthRange)
    val day2 = readDay(startDay + 1, latLonBounds, depthRange)
    grids.enqueue((day1, startDay, getDaysInMonth))
    grids.enqueue((day2, startDay + 1, getDaysInMonth))
  }

  def next(): FlowGridWrapper = {
    clearYesterday()
    if (isEndOfMonth && hasNext) readMonth()
    getNextFlowData()
    new FlowGridWrapper(depths, grids.map(grid => grid._1.toList).toList)
  }

  def hasNext: Boolean = currentFile < files.size - 1

  def closeAllOpenDatasets(): Unit = {
    if (dataset.nonEmpty) {
      dataset.foreach(dataset => dataset._1.close())
    }
  }

  private def clearYesterday(): Unit = {
    if (grids.size == 2) {
      grids.dequeue
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
    val nextDayData = ListBuffer.empty[(GridDataset, String)]
    variables.foreach(
      variable => nextDayData += ((loadNextFlowFile(variable), variable))
    )
    closeAllOpenDatasets()
    dataset = nextDayData.toList
    currentFile += 1
  }

  private def getNextFlowData(): Unit = {
    val nextDay = readDay(getNextDayOfMonth, latLonBounds, depthRange)
    grids.enqueue((nextDay, getNextDayOfMonth, getDaysInMonth))
  }

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
    (
      files,
      files.indexWhere(
        file =>
          isFileForDate(
            flow.period.getStart.toLocalDate,
            getDateFromFileName(file)
          )
      )
    )
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

  private def getDayOfMonth: Int = grids.front._2

  private def getNextDayOfMonth: Int = isEndOfMonth match {
    case true  => 1
    case false => getDayOfMonth + 1
  }

  private def getDaysInMonth: Int =
    dataset.head._1.getCalendarDateEnd.getDayOfMonth()

  private def isEndOfMonth: Boolean = getDayOfMonth == grids.front._3

}

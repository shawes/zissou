package io

import java.io.File
import scala.collection.mutable.{ListBuffer, Queue}
import com.github.nscala_time.time.Imports._
import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.{GeoGrid, GridDataset}
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}

class FlowFileIterator(val netcdfFolder: String, val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  val variables = List("u", "v", "w") //TODO: Put this into the config file
  val datasets = new Queue[List[(GridDataset, String)]]
  val grids = new Queue[List[(Array[Array[Array[Float]]], GridCoordSystem)]]
  val depths = List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0) //TODO: Get this info from the netcdf file
  var currentDate = flow.period.getStart
  var day : Int = currentDate.dayOfMonth.get
  var days = 1
  val netcdfHandler = new NetcdfFileHandler()
  val getFileInformation = initialiseFiles()
  var currentFile: Int = getFileInformation._2
  val files = getFileInformation._1
  val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 180.0))
  val depthRange: Range = new Range(0, 14)

  def next(): FlowGridWrapper = {
    debug("Loading the next flow timestep")
    checkIfInitialFlow()
    clearOldGrid()
    checkIfEndOfMonthLoadNextFile()
    getNextFlowData()
    incrementDayCounter()
    new FlowGridWrapper(depths, grids.toList)
  }

  def hasNext: Boolean = currentFile < files.size - 1

  def closeAllOpenDatasets(): Unit = {
    if (datasets.nonEmpty) {
      datasets.foreach(dataset => dataset.head._1.close())
      datasets.clear()
    }
  }

  def shutdown() : Unit = {
    closeAllOpenDatasets()
    netcdfHandler.shutdown()
  }

  private def checkIfInitialFlow() : Unit = {
    if(datasets.isEmpty) {
      val firstday : ListBuffer[(GridDataset, String)] = ListBuffer.empty
      variables.foreach(variable => firstday += ((loadNextFlowFile(variable), variable)))
      datasets.enqueue(firstday.toList)
      getDaysInDataset()
      debug(s"Current file is $currentFile")
    }
  }

  private def clearOldGrid() : Unit = {
    if(startOfMonth && datasets.size == 2) {
      val old = datasets.dequeue
      old.map(file => file._1.close())
    }
  }

  private def checkIfEndOfMonthLoadNextFile() : Unit = {
    if (endOfMonth && hasNext) {
      debug("Reached the end of the month")
      currentFile += 1
      val nextday = ListBuffer.empty[(GridDataset, String)]
      variables.foreach(variable => nextday += ((loadNextFlowFile(variable), variable)))
      if(datasets.size == 2) {
        val old = datasets.dequeue
        old.map(file => file._1.close())
      }
      datasets.enqueue(nextday.toList)
      debug(s"Current file is $currentFile")
    }
  }

  private def getNextFlowData() : Unit = {
    val subset = if (endOfMonth) {
      getGeoGridsFromAcrossMonths(latlonBounds, depthRange)
    } else {
      val timeRange: Range = new Range(nextDay, nextDay)
      getGeoGridsFromWithinMonth(latlonBounds, timeRange, depthRange)
    }

    val data = subset.map(dataset => (dataset.readDataSlice(0,-1,-1,-1).copyToNDJavaArray().asInstanceOf[Array[Array[Array[Float]]]], dataset.getCoordinateSystem()))

    if(grids.size == 2) {
      grids.dequeue
    }
    grids.enqueue(data)
  }

  // Get two grids that occur in the same file
  private def getGeoGridsFromWithinMonth(latlonBounds: LatLonRect, timeRange: Range, depthRange: Range): List[GeoGrid] = {
    datasets.head.map(dataset => dataset._1.findGridByName(dataset._2).subset(timeRange, depthRange, latlonBounds, 0, 0, 0)).toList
  }

  // Gets the last grid from one file and the first from the next file
  private def getGeoGridsFromAcrossMonths(latlonBounds: LatLonRect, depthRange: Range): List[GeoGrid] = datasets.last.map(dataset => dataset._1.findGridByName(dataset._2).subset(new Range(0, 0), depthRange, latlonBounds, 0, 0, 0)).toList

  private def endOfMonth: Boolean = day == days

  private def startOfMonth: Boolean = day == 1

  private def nextDay: Int = day + 1

  private def incrementDayCounter(): Unit = {
    if(day < days) day += 1
    else day = 1
  }

  private def loadNextFlowFile(prefix: String): GridDataset = {
    val path = netcdfFolder + "/" + prefix
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    val filename = path + "/" + files(currentFile)
    debug(s"Loading the next file $filename")
    netcdfHandler.openLocalFile(filename)
  }

  private def initialiseFiles(): (Array[String], Int) = {
    val path = netcdfFolder + "/" + "u"
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    (files, files.indexWhere(file => isFileforDate(currentDate,getDateFromFileName(file))))
  }

  private def getDateFromFileName(filename : String) : DateTime = {
    debug(s"Getting date from file name $filename")
    val prefix = filename.split('.')
    val sections = prefix(0).split('_')
    new DateTime(sections(2).toInt, sections(3).toInt, 1, 1, 1)
  }

  private def isFileforDate(startDate : DateTime, fileDate : DateTime) : Boolean =  {
    startDate.year == fileDate.year && startDate.monthOfYear.get == fileDate.monthOfYear.get
  }

  private def getDaysInDataset() : Unit = {
    val grid = datasets.head.head._1.findGridByName(datasets.head.head._2)
    val shape = grid.getShape
    days = shape(0) - 1
  }

}

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
  val grids = new Queue[(List[(Array[Array[Array[Float]]], GridCoordSystem)],Int)]
  //val grids = new Queue[List[GeoGrid]]
  val depths = List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0) //TODO: Get this info from the netcdf file
  var currentDate = flow.period.getStart.toLocalDate
  var day : Int = currentDate.dayOfMonth.get
  var days = 0
  val netcdfHandler = new NetcdfFileHandler()
  val getFileInformation = initialiseFiles()
  var currentFile: Int = getFileInformation._2
  val files = getFileInformation._1
  val latlonBounds = new LatLonRect(new LatLonPointImpl(-50.0, 142.0), new LatLonPointImpl(-10.0, 180.0))
  val depthRange: Range = new Range(0, 14)
  //checkIfInitialFlow()

  def next(): FlowGridWrapper = {
    if(grids.isEmpty) {
      checkIfEndOfMonthLoadNextFile()
      checkIfInitialFlow()
      incrementDayCounter()
    } else {
      incrementDayCounter()
      clearOldGrid()
      checkIfEndOfMonthLoadNextFile()
      //info("Loading the next flow timestep for day: " + day + " of days " + days)
      getNextFlowData()
    }
    new FlowGridWrapper(depths, grids.map(grid => grid._1.toList).toList)
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
      //info(s"Current file is $currentFile")
    }
    val day1 = getGeoGridsFromWithinMonth(latlonBounds, new Range(day-1, day-1), depthRange)
    val day2 = getGeoGridsFromWithinMonth(latlonBounds, new Range(day, day), depthRange)

     val data1 = day1.map(grid => (grid.readDataSlice(0,-1,-1,-1).copyToNDJavaArray().asInstanceOf[Array[Array[Array[Float]]]], grid.getCoordinateSystem()))

     val data2 = day2.map(grid => (grid.readDataSlice(0,-1,-1,-1).copyToNDJavaArray().asInstanceOf[Array[Array[Array[Float]]]], grid.getCoordinateSystem()))

     grids.enqueue((data1,day))
     grids.enqueue((data2,day+1))

     getDaysInDataset()

     //info("INIT FLOW DATA: there are "+ grids.size +" grids of days ids: " + grids.head._2 +", "+grids.last._2)
  }

  private def clearOldGrid() : Unit = {
    if(startOfMonth && datasets.size == 2) {
      val old = datasets.dequeue
      old.map(file => file._1.close())
    }
    getDaysInDataset()
  }

  private def checkIfEndOfMonthLoadNextFile() : Unit = {
    if (endOfMonth(day) && hasNext) {
      val nextDayData = ListBuffer.empty[(GridDataset, String)]
      variables.foreach(variable => nextDayData += ((loadNextFlowFile(variable), variable)))
      if(datasets.size > 1) {
        val old = datasets.dequeue
        old.map(file => file._1.close())
      }
      datasets.enqueue(nextDayData.toList)
      getDaysInDataset()
      currentFile += 1
    }

  }

  private def getNextFlowData() : Unit = {
    val subset = if (endOfMonth(day)) {
      getGeoGridsFromAcrossMonths(latlonBounds, depthRange)
    } else {
      val timeRange: Range = new Range(day, day)
      getGeoGridsFromWithinMonth(latlonBounds, timeRange, depthRange)
    }

     val data = subset.map(grid => (grid.readDataSlice(0,-1,-1,-1).copyToNDJavaArray().asInstanceOf[Array[Array[Array[Float]]]], grid.getCoordinateSystem()))

     if(grids.size > 1) grids.dequeue

    grids.enqueue((data,day))


      //info("FLOW DATA: there are "+ grids.size+" grids of days ids: " + grids.head._2 +", "+grids.last._2 +", datasets size is  "+ datasets.size)
  }

  // Get two grids that occur in the same file
  private def getGeoGridsFromWithinMonth(latlonBounds: LatLonRect, timeRange: Range, depthRange: Range): List[GeoGrid] = {
    getDaysInDataset()
    datasets.head.map(dataset => dataset._1.findGridByName(dataset._2).subset(timeRange, depthRange, latlonBounds, 0, 0, 0)).toList
  }

  // Gets the last grid from one file and the first from the next file
  private def getGeoGridsFromAcrossMonths(latlonBounds: LatLonRect, depthRange: Range): List[GeoGrid] =  {
    //info("Datasets size is  "+ datasets.size)
    datasets.last.map(dataset => dataset._1.findGridByName(dataset._2).subset(new Range(0, 0), depthRange, latlonBounds, 0, 0, 0)).toList
  }

  private def endOfMonth(day : Int): Boolean = day == days
  private def startOfMonth: Boolean = day == 1

  private def incrementDayCounter(): Unit = {
    if(day < days) {
      day += 1
    } else {
      //info("day is "+day+", and days is"+days)
      day = 1
      getDaysInDataset()
    }
  }

  private def loadNextFlowFile(prefix: String): GridDataset = {
    val path = netcdfFolder + "/" + prefix
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    val filename = path + "/" + files(currentFile)
    //info(s"Loading the next file $filename")
    netcdfHandler.openLocalFile(filename)
  }

  private def initialiseFiles(): (Array[String], Int) = {
    val path = netcdfFolder + "/" + "u"
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    (files, files.indexWhere(file => isFileforDate(currentDate,getDateFromFileName(file))))
  }

  private def getDateFromFileName(filename : String) : LocalDate = {
    debug(s"Getting date from file name $filename")
    val prefix = filename.split('.')
    val sections = prefix(0).split('_')
    new LocalDateTime(sections(2).toInt, sections(3).toInt, 1, 1, 1).toLocalDate
  }

  private def isFileforDate(startDate : LocalDate, fileDate : LocalDate) : Boolean =  {
    startDate.year == fileDate.year && startDate.monthOfYear.get == fileDate.monthOfYear.get
  }

  private def getDaysInDataset() : Unit = {
    val grid = datasets.head.head._1.findGridByName(datasets.head.head._2)
    val shape = grid.getShape
    //info("Number of days in month is " + shape(0))
    days = shape(0)
  }

}

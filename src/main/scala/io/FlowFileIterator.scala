package io

import java.io.File

import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.grid.{GeoGrid, GridDataset}
import ucar.nc2.dt.GridCoordSystem
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}
import com.github.nscala_time.time.Imports._

import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

class FlowFileIterator(val netcdfFolder: String, val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  val variables = List("u", "v", "w")
  val datasets = new Queue[List[(GridDataset, String)]]
  val grids = new Queue[List[(Array[Array[Array[Float]]], GridCoordSystem)]]
  val depths = List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0)
  var currentFile: Int = 0
  var day = 0 //flow.period.start.getDays
  var days = 0
  var numberOfFiles = Int.MaxValue
  val netcdfHandler = new NetcdfFileHandler()

  def next(): FlowGridWrapper = {
    debug("Getting the next day")

    if(datasets.isEmpty) {
      val firstday : ListBuffer[(GridDataset, String)] = ListBuffer.empty
      variables.foreach(variable => firstday += ((loadNextFile(variable), variable)))
      datasets.enqueue(firstday.toList)
      getDaysInDataset()
    }

    if(startOfMonth && datasets.size == 2) {
      val old = datasets.dequeue
      old.map(file => file._1.close())
    }

    if (endOfMonth) {
      debug("Reached the end of the month")
      currentFile += 1
      val nextday = ListBuffer.empty[(GridDataset, String)]
      variables.foreach(variable => nextday += ((loadNextFile(variable), variable)))
      if(datasets.size == 2) {
        val old = datasets.dequeue
        old.map(file => file._1.close())
      }
      datasets.enqueue(nextday.toList)
    }


    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 180.0))
    val depthRange: Range = new Range(0, 14)

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

    val gridwrapper = new FlowGridWrapper(depths, grids.toList)
    incrementDayCounter()
    gridwrapper
  }


  private def getGeoGridsFromWithinMonth(latlonBounds: LatLonRect, timeRange: Range, depthRange: Range): List[GeoGrid] = {
    datasets.head.map(dataset => dataset._1.findGridByName(dataset._2).subset(timeRange, depthRange, latlonBounds, 0, 0, 0)).toList
  }

  private def getGeoGridsFromAcrossMonths(latlonBounds: LatLonRect, depthRange: Range): List[GeoGrid] = {

    val startOfNextMonthGrid = datasets.last.map(dataset => dataset._1.findGridByName(dataset._2).subset(new Range(0, 0), depthRange, latlonBounds, 0, 0, 0))

    startOfNextMonthGrid.toList

  }

  private def endOfMonth: Boolean = day == days

  private def startOfMonth: Boolean = day == 0

  private def nextDay: Int = day + 1

  private def incrementDayCounter(): Unit = {
    if (day < days) {
      day += 1
    } else {
      day = 0
      //closeAllOpenDatasets()
    }
  }

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

  private def loadNextFile(prefix: String): GridDataset = {
    val path = netcdfFolder + "/" + prefix
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    numberOfFiles = files.length
    val filename = path + "/" + files(currentFile)
    info(s"$filename")
    netcdfHandler.openLocalFile(filename)
  }

  private def getDaysInDataset() : Unit = {
    val grid = datasets.head.head._1.findGridByName(datasets.head.head._2)
    val shape = grid.getShape
    days = shape(0) - 1
    debug("There are this many days in the month: " + days)
  }

  def hasNext: Boolean = currentFile < numberOfFiles
}

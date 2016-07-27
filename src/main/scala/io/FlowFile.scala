package io

import java.io.File

import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.grid.{GeoGrid, GridDataset}
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}

import scala.collection.mutable.ListBuffer

class FlowFile(val netcdfFolder: String, val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  val variables = List("u", "v", "w")
  val datasets = ListBuffer(ListBuffer.empty[(GridDataset, String)])
  val depths = List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0)
  var currentFile: Int = 0
  var day = 0
  var days = 0
  var numberOfFiles = Int.MaxValue

  def next(): FlowGridWrapper = {
    debug("Getting the next day")

    if (startOfMonth) {
      //closeAllOpenDatasets()
      variables.foreach(variable => datasets.head += ((loadNextFile(variable), variable)))
      //currentFile += 1
      updateDayCounters()
    } else {
      if (endOfMonth) {
        currentFile += 1
        val nextday = ListBuffer.empty[(GridDataset, String)]
        variables.foreach(variable => nextday += ((loadNextFile(variable), variable)))
        datasets += nextday
      }
    }

    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))
    val depthRange: Range = new Range(0, 14)



    val datasetsSubset = if (endOfMonth) {
      getGeoGridsFromGridDatasets(latlonBounds, depthRange)
    } else {
      val timeRange: Range = new Range(day, nextDay)
      getGeoGridsFromGridDataset(latlonBounds, timeRange, depthRange)
    }

    val gridwrapper = new FlowGridWrapper(datasetsSubset.head.getCoordinateSystem, depths, datasetsSubset)
    incrementDayCounter()
    gridwrapper
  }


  private def getGeoGridsFromGridDataset(latlonBounds: LatLonRect, timeRange: Range, depthRange: Range): List[GeoGrid] = {
    //val grids = variables.map( variable => datasets.find)
    datasets.head.map(dataset => dataset._1.findGridByName(dataset._2).subset(timeRange, depthRange, latlonBounds, 0, 0, 0)).toList
  }

  private def getGeoGridsFromGridDatasets(latlonBounds: LatLonRect, depthRange: Range): List[GeoGrid] = {
    //val grids = variables.map( variable => datasets.find)
    List(datasets.head.map(dataset => dataset._1.findGridByName(dataset._2).subset(new Range(day, day), depthRange, latlonBounds, 0, 0, 0)).head,
      datasets(1).map(dataset => dataset._1.findGridByName(dataset._2).subset(new Range(0, 0), depthRange, latlonBounds, 0, 0, 0)).head)
  }

  private def endOfMonth: Boolean = day == days

  private def startOfMonth: Boolean = day == 0

  private def nextDay: Int = day + 1

  private def incrementDayCounter(): Unit = {
    if (day < days) {
      day += 1
    } else {
      day = 0
      closeAllOpenDatasets()
    }
  }

  def closeAllOpenDatasets(): Unit = {
    if (datasets.nonEmpty) {
      datasets.foreach(dataset => dataset.head._1.close())
      datasets.clear()
    }
  }

  private def loadNextFile(prefix: String): GridDataset = {
    val path = netcdfFolder + "/" + prefix
    val netcdfFile = new NetcdfFileHandler
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    debug("list of file has " + files.length)
    numberOfFiles = files.length
    netcdfFile.openLocalFile(path + "/" + files(currentFile))
  }

  private def updateDayCounters() {
    val grid = datasets.head.head._1.findGridByName(datasets.head.head._2)
    val shape = grid.getShape
    days = shape(0) - 1
  }

  def hasNext: Boolean = currentFile <= numberOfFiles
}


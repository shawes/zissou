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
  val datasets = ListBuffer.empty[GridDataset]
  var currentFile: Int = 0
  var day = 0
  var days = 0
  var numberOfFiles = Int.MaxValue

  def next(): FlowGridWrapper = {
    debug("Getting the next day")

    if (day == 0) {
      closeAllOpenDatasets()
      variables.foreach(variable => datasets :+ loadNextFile(variable))
      currentFile += 1
      updateDayCounters()
    }

    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))
    val timeRange: Range = new Range(day, day + 1)
    val depthRange: Range = new Range(0, 15)


    incrementDayCounter()

    new FlowGridWrapper(datasets.head.getGrids.get(0).asInstanceOf[GeoGrid].getCoordinateSystem,
      null,
      datasets.head.getGrids.get(0).asInstanceOf[GeoGrid].subset(timeRange, depthRange, latlonBounds, 0, 0, 0),
      datasets(1).getGrids.get(0).asInstanceOf[GeoGrid].subset(timeRange, depthRange, latlonBounds, 0, 0, 0),
      datasets(2).getGrids.get(0).asInstanceOf[GeoGrid].subset(timeRange, depthRange, latlonBounds, 0, 0, 0))
  }

  private def incrementDayCounter(): Unit = {
    if (day != days) {
      day += 1
    } else {
      day = 0
    }
  }

  def closeAllOpenDatasets(): Unit = {
    if (datasets.nonEmpty) {
      datasets.foreach(dataset => dataset.close())
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
    val grid = datasets.head.getGrids.get(0).asInstanceOf[GeoGrid]
    val shape = grid.getShape
    days = shape(0)
  }

  def hasNext: Boolean = currentFile < numberOfFiles




}


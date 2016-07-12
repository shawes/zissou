package io

import java.io.File

import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.grid.{GeoGrid, GridDataset}
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}

class FlowFile(val netcdfFolder: String, val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  var currentFile: Int = 0
  var day = 0
  var days = 0
  var numberOfFiles = Int.MaxValue
  var uDataset, vDataset, wDataset: GridDataset = null

  def next(): FlowGridWrapper = {


    var filesMax = 0
    if (day == 0) {
      if (uDataset != null) {
        uDataset.close()
        vDataset.close()
        wDataset.close()
      }
      uDataset = loadNextFile("u")
      vDataset = loadNextFile("v")
      wDataset = loadNextFile("w")
      currentFile += 1
      updateDayCounters()
    }

    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))
    val timeRange: Range = new Range(day, day + 1)
    val depthRange: Range = new Range(0, 15)


    if (day != days) {
      day += 1
    } else {
      day = 0
    }

    new FlowGridWrapper(uDataset.getGrids.get(0).asInstanceOf[GeoGrid].getCoordinateSystem,
      null,
      uDataset.getGrids.get(0).asInstanceOf[GeoGrid].subset(timeRange, depthRange, latlonBounds, 0, 0, 0),
      vDataset.getGrids.get(0).asInstanceOf[GeoGrid].subset(timeRange, depthRange, latlonBounds, 0, 0, 0),
      wDataset.getGrids.get(0).asInstanceOf[GeoGrid].subset(timeRange, depthRange, latlonBounds, 0, 0, 0))
  }

  private def loadNextFile(prefix: String): GridDataset = {
    val path = netcdfFolder + "/" + prefix
    val netcdfFile = new NetcdfFileHandler
    val files = new File(path).list().filter(p => p.endsWith(NetcdfExtension))
    debug("list of file has " + files.length)
    numberOfFiles = files.length
    //val file = skipHiddenAndSystemFiles(files)
    val dataset = netcdfFile.openLocalFile(path + "/" + files(currentFile))
    dataset
  }

  private def updateDayCounters() {
    val grid = uDataset.getGrids.get(0).asInstanceOf[GeoGrid]
    day = 0
    val shape = grid.getShape
    days = shape(0)
  }

  def hasNext: Boolean = currentFile < numberOfFiles




}


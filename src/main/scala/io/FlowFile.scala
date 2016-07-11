package io

import java.io.File

import grizzled.slf4j._
import physical.flow.{Flow, FlowGridWrapper}
import ucar.ma2.Range
import ucar.nc2.dt.grid.GeoGrid
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}

class FlowFile(val netcdfFolder: String, val flow: Flow) extends Logging {
  val NetcdfExtension = ".nc"
  //val files: Array[String] = inputs.flowFiles
  var currentFile: Int = 0
  var day = 0
  var days = 0
  var numberOfFiles = 0

  def next(): FlowGridWrapper = {

    var uDataset, vDataset, wDataset: GeoGrid = null
    var filesMax = 0
    if (day == 0) {
      uDataset = loadNextFile("u")
      vDataset = loadNextFile("v")
      wDataset = loadNextFile("w")
      currentFile += 1
    }

    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))
    val timeRange: Range = new Range(day, day + 1)
    val depthRange: Range = new Range(0, 15)


    if (day != days) {
      day += 1
    } else {
      day = 0
    }

    new FlowGridWrapper(uDataset.getCoordinateSystem,
      null,
      uDataset.subset(timeRange, depthRange, latlonBounds, 0, 0, 0),
      vDataset.subset(timeRange, depthRange, latlonBounds, 0, 0, 0),
      wDataset.subset(timeRange, depthRange, latlonBounds, 0, 0, 0))



    /*if(depth.averageOverAllDepths) {
       polygons = averageDepthDimension(polygons)
    }*/

  }

  private def loadNextFile(prefix: String): GeoGrid = {
    val netcdfFile = new NetcdfFileHandler
    val files = new File(netcdfFolder + "/" + prefix).list().filter(p => p.endsWith(NetcdfExtension))
    numberOfFiles = files.length
    //val file = skipHiddenAndSystemFiles(files)
    val grid = netcdfFile.openLocalFile(files(currentFile))
    day = 0
    val shape = grid.getShape
    days = shape(0)
    grid
  }

  //private def updateFlowWithDimensions(properties)

  /*  private def averageDepthDimension(polygons: FlowGridWrapper): FlowGridWrapper= {
      val cellCount: Int = flow.dimensions.cellSize.width * flow.dimensions.cellSize.height
      val averagedPolygons = ArrayBuffer.empty[FlowPolygon]
      for (i <- 0 until cellCount) {
        var sumU: Double = 0.0
        var sumV: Double = 0.0
        var sumSalt: Double = 0.0
        var sumTemp: Double = 0.0
        var count: Int = 0
        var isLand = true

        for (j <- 0 until flow.dimensions.cellSize.depth) {
          val polygon = polygons(i + (j * cellCount))
          if (!polygon.isLand) {
            isLand = false
            sumU += polygon.velocity.u
            sumV += polygon.velocity.v
            sumSalt += polygon.salinity
            sumTemp += polygon.temperature
            count += 1
          }
        }
        val averagedPolygon = polygons(i)
        averagedPolygon.isLand = isLand

        if (!isLand) {
          averagedPolygon.velocity.u = sumU / count
          averagedPolygon.velocity.v = sumV / count
          averagedPolygon.salinity = sumSalt / count
          averagedPolygon.temperature = sumTemp / count

        }
        averagedPolygons += averagedPolygon
      }
      averagedPolygons.toArray
    }*/

  def hasNext: Boolean = currentFile < numberOfFiles


}


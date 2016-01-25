package io

import grizzled.slf4j._
import physical.flow.{Depth, Flow, FlowPolygon}

import scala.collection.mutable.ArrayBuffer

class FlowReader(val inputs: InputFiles, val depth: Depth) {
  val files: Array[String] = inputs.flowFiles.toArray
  val logger = Logger(classOf[FlowReader])
  var currentFile: Int = 0
  var flow = new Flow(depth)

  def next(): Array[FlowPolygon] = {
    val polygons = loadNextFile()
    currentFile += 1
    /*if(depth.averageOverAllDepths) {
       polygons = averageDepthDimension(polygons)
    }*/
    polygons.toArray
  }

  private def loadNextFile(): Array[FlowPolygon] = {
    val reader = new FlowXmlReader(flow)
    val file = skipHiddenAndSystemFiles(files(currentFile))

    logger.debug("Reading in " + file + " from " + files.toString)
    val polygons = reader.read(inputs.flowFilePath + "/" + file)
    flow = reader.oceanData
    if (flow.depth.average) averageDepthDimension(polygons) else polygons
  }

  private def skipHiddenAndSystemFiles(file: String): String = {
    var tempFile = file
    while (!tempFile.endsWith(".xml")) {
      currentFile += 1
      tempFile = files(currentFile)
    }
    tempFile
  }

  private def averageDepthDimension(polygons: Array[FlowPolygon]): Array[FlowPolygon] = {
    val cellCount: Int = flow.grid.width * flow.grid.height
    val averagedPolygons = ArrayBuffer.empty[FlowPolygon]
    for (i <- 0 until cellCount) {
      var sumU: Double = 0.0
      var sumV: Double = 0.0
      var sumSalt: Double = 0.0
      var sumTemp: Double = 0.0
      var count: Int = 0
      var isLand = true

      for (j <- 0 until flow.grid.depth) {
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
  }

  def hasNext : Boolean = currentFile < files.length


}


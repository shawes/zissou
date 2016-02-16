package io

import java.io.File

import grizzled.slf4j._
import physical.flow.{Flow, FlowPolygon}

import scala.collection.mutable.ArrayBuffer

class FlowReader(val inputs: InputFiles, val flow: Flow) extends Logging {
  val files: Array[String] = inputs.flowFiles.toArray
  var currentFile: Int = 0


  def next(): Array[FlowPolygon] = {
    val polygons = loadNextFile()
    currentFile += 1
    /*if(depth.averageOverAllDepths) {
       polygons = averageDepthDimension(polygons)
    }*/
    polygons.toArray
  }

  private def loadNextFile(): Array[FlowPolygon] = {
    val flowXmlReader = new FlowXmlReader()
    val file = skipHiddenAndSystemFiles(files(currentFile))

    debug("Reading in " + file + " from " + files.toString)
    val polygons = flowXmlReader.read(new File(inputs.flowFilePath + "/" + file))
    flow.dimensions = flowXmlReader.flowDimensions
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

  //private def updateFlowWithDimensions(properties)

  private def averageDepthDimension(polygons: Array[FlowPolygon]): Array[FlowPolygon] = {
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
  }

  def hasNext : Boolean = currentFile < files.length


}


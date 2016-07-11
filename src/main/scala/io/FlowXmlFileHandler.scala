package io

import java.io.File

import grizzled.slf4j._
import maths.ContinuousRange
import physical.flow.{Dimensions, FlowPolygon}
import physical.{Cell, GeoCoordinate, Grid, Velocity}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.xml.MetaData
import scala.xml.pull._

/** Parses the flow xml config generated from the net-cdf config
  *
  * @constructor create a new flow file xml reader
  */
class FlowXmlFileHandler() extends Logging {

  var flowDimensions = new Dimensions(new ContinuousRange(), new ContinuousRange(), new ContinuousRange(), new Grid())
  /** Reads in the XML from a file
    *
    * @param file the location of the XML file
    * @return a vector of flow polygons comprising the data from the XML file
    */
  def read(file: File): Array[FlowPolygon] = {
    val src = Source.fromFile(file)
    val reader = new XMLEventReader(src)
    readXmlElements(reader)
  }

  private def readXmlElements(xml: XMLEventReader): Array[FlowPolygon] = {
    var polygons: ArrayBuffer[FlowPolygon] = ArrayBuffer.empty
    var polygon: FlowPolygon = new FlowPolygon()

    while (xml.hasNext) {
      xml.next() match {
        case EvElemStart(_, "step", attributes, _) =>
          val timestep = attributes("timestep").text
        case EvElemStart(_, "dimensions", attributes, _) =>
          flowDimensions.cellSize.width = attributes("longitude").text.toInt
          flowDimensions.cellSize.height = attributes("latitude").text.toInt
          flowDimensions.cellSize.depth = attributes("depth").text.toInt
        case EvElemStart(_, "latitudeRange", attributes, _) =>
          flowDimensions.latitudeBoundary =
            new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
        case EvElemStart(_, "longitudeRange", attributes, _) =>
          flowDimensions.longitudeBoundary =
            new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
        case EvElemStart(_, "depthRange", attributes, _) =>
          flowDimensions.depth =
            new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
        case EvElemStart(_, "cellRange", attributes, _) =>
          flowDimensions.cellSize.cell =
            new Cell(attributes("width").text.toDouble,
              attributes("width").text.toDouble,
              attributes("depth").text.toDouble)
        case EvElemStart(_, "flow", attributes, _) =>
          polygon = new FlowPolygon()
          polygon.id = attributes("id").text.toInt
        case EvElemStart(_, "depth", attributes, _) =>
          polygon.centroid = new GeoCoordinate(0, 0, attributes.value.head.toString().toDouble)
        case EvElemStart(_, "salt", attributes, _) =>
          polygon.salinity = attributes.value.head.toString().toDouble
        case EvElemStart(_, "temp", attributes, _) =>
          polygon.temperature = attributes.value.head.toString().toDouble
        case EvElemStart(_, "velocity", attributes, _) =>
          polygon.velocity = readVelocityElement(attributes)
        case EvElemStart(_, "locus", attributes, _) =>
          val locus = readLocusElement(attributes)
          polygon.centroid = new GeoCoordinate(locus.latitude, locus.longitude, polygon.centroid.depth)
          constructArakawaAGrid(polygon, locus, flowDimensions.cellSize.cell.width * 0.5)
        case EvElemEnd(_, "flow") =>
          polygons += polygon
        case _ => ()
      }
    }
    xml.stop()
    polygons.toArray
  }

  private def readVelocityElement(velocities: MetaData): Velocity = {
    val u = velocities("u").text.toDouble
    val v = velocities("v").text.toDouble
    val w = velocities("w").text.toDouble
    new Velocity(u, v, w)
  }

  private def readLocusElement(coordinates: MetaData): GeoCoordinate = {
    val longitude = coordinates("lon").text.toDouble
    val latitude = coordinates("lat").text.toDouble
    new GeoCoordinate(latitude, longitude)
  }

  private def constructArakawaAGrid(polygon: FlowPolygon, locus: GeoCoordinate, halfLength: Double) {
    polygon.vertices += new GeoCoordinate(locus.latitude - halfLength, locus.longitude - halfLength)
    polygon.vertices += new GeoCoordinate(locus.latitude - halfLength, locus.longitude + halfLength)
    polygon.vertices += new GeoCoordinate(locus.latitude + halfLength, locus.longitude + halfLength)
    polygon.vertices += new GeoCoordinate(locus.latitude + halfLength, locus.longitude - halfLength)
  }

}



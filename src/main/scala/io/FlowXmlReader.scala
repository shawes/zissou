package io

import java.io.File

import grizzled.slf4j._
import maths.ContinuousRange
import physical.flow.{Flow, FlowPolygon}
import physical.{Cell, GeoCoordinate, Velocity}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.xml.MetaData
import scala.xml.pull._

/** Parses the flow xml config generated from the net-cdf config
  *
  * @constructor create a new flow file xml reader
  * @param oceanData defines the oceanography
  */
class FlowXmlReader(val oceanData: Flow) {

  val logger = Logger(classOf[FlowXmlReader])

  /** Reads in the XML from a file
    *
    * @param filePath the location of the XML file
    * @return a vector of flow polygons comprising the data from the XML file
    */
  def read(filePath: String): Vector[FlowPolygon] = {
    logger.debug("File path is " + filePath)
    val src = Source.fromFile(new File(filePath))
    val reader = new XMLEventReader(src)
    //printElements(reader)
    readXmlElements(reader)
  }

  private def readXmlElements(xml: XMLEventReader): Vector[FlowPolygon] = {
    var polygons: ArrayBuffer[FlowPolygon] = ArrayBuffer.empty
    var polygon: FlowPolygon = new FlowPolygon()

    while (xml.hasNext) {
      xml.next() match {
        case EvElemStart(_, "step", attributes, _) =>
          val timestep = attributes("timestep").text
        case EvElemStart(_, "dimensions", attributes, _) =>
          oceanData.grid.width = attributes("longitude").text.toInt
          oceanData.grid.height = attributes("latitude").text.toInt
          oceanData.grid.depth = attributes("depth").text.toInt
        case EvElemStart(_, "latitudeRange", attributes, _) =>
          oceanData.latitudeRange =
            new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
        case EvElemStart(_, "longitudeRange", attributes, _) =>
          oceanData.longitudeRange =
            new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
        case EvElemStart(_, "depthRange", attributes, _) =>
          oceanData.depth.range =
            new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
        case EvElemStart(_, "cellRange", attributes, _) =>
          oceanData.grid.cell =
            new Cell(attributes("width").text.toDouble,
              attributes("width").text.toDouble,
              attributes("depth").text.toDouble)
        case EvElemStart(_, "flow", attributes, _) =>
          polygon = new FlowPolygon()
          polygon.id = attributes("id").text.toInt
        case EvElemStart(_, "depth", attributes, _) =>
          val depth = attributes.value.head.toString().toDouble
          polygon.centroid = new GeoCoordinate(0, 0, depth)
        case EvElemStart(_, "salt", attributes, _) =>
          polygon.salinity = attributes.value.head.toString().toDouble
        case EvElemStart(_, "temp", attributes, _) =>
          polygon.temperature = attributes.value.head.toString().toDouble
        case EvElemStart(_, "velocity", attributes, _) =>
          polygon.velocity = readVelocityElement(attributes)
        case EvElemStart(_, "locus", attributes, _) =>
          val locus = readLocusElement(attributes)
          polygon.centroid.latitude = locus.latitude
          polygon.centroid.longitude = locus.longitude
          constructArakawaAGrid(polygon, locus, oceanData.grid.cell.width * 0.5)
        case EvElemEnd(_, "flow") =>
          polygons += polygon
        case _ => ()
      }
    }
    xml.stop()
    logger.debug("Returning " + polygons.size + " polygons")
    polygons.toVector
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

  private def printElements(xml: XMLEventReader) {
    while (xml.hasNext) logger.trace(xml.next().toString)
  }

}



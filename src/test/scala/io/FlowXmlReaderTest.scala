package io

import org.scalatest.{PrivateMethodTester, FlatSpec}
import org.scalatest.mock.MockitoSugar
import physical.flow.{FlowPolygon, Flow}
import scala.xml.pull.XMLEventReader
import scala.io.Source
import physical.{GeoCoordinate, Cell}

class FlowXmlReaderTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The flow xml reader" should "parse the flow node of id = 1" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.length == 1)
    assert(elements.head.id == 1)
  }

  it should "parse multiple flow nodes" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(twoFlowNodesXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.length == 2, "There should be two elements here")
  }

  it should "parse depth node in the properties" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowWithPropertiesXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    reader invokePrivate readFlowNodes(xml)
    assert(flow.depth.range.start == 0)
    assert(flow.depth.range.end == 100)
  }

  it should "parse latitude range node in the properties" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowWithPropertiesXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    reader invokePrivate readFlowNodes(xml)
    assert(flow.latitudeRange.start == -40.0)
    assert(flow.latitudeRange.end == -10.0)
  }

  it should "parse longitude range node in the properties" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowWithPropertiesXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    reader invokePrivate readFlowNodes(xml)
    assert(flow.longitudeRange.start == 142.0)
    assert(flow.longitudeRange.end == 162.0)
  }

  it should "parse cell range node in the properties" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowWithPropertiesXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    reader invokePrivate readFlowNodes(xml)
    assert(flow.grid.cell.width == 0.1)
    assert(flow.grid.cell.height == 0.1)
    assert(flow.grid.cell.depth == 10.0)
  }

  it should "parse the properties attributes" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowWithPropertiesXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(flow.grid.width == 201)
    assert(flow.grid.height == 301)
    assert(flow.grid.depth == 11)
    assert(elements.length > 0)
  }

  it should "parse the depth node" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.head.centroid.depth == 5.01)
  }

  it should "parse the salinity node" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.head.salinity == 35.24)
  }

  it should "parse the temperature node" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.head.temperature == 14.85)
  }

  it should "parse the velocity node" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.head.velocity.u == -0.18)
    assert(elements.head.velocity.v == 0.05)
    assert(elements.head.velocity.w == 7.274356E-7)
  }

  it should "parse the locus node" in {
    var flow = new Flow()
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.head.centroid.latitude == -40.000534)
    assert(elements.head.centroid.longitude == 142.00035)
  }

  it should "construct okawana grid from locus" in {
    var flow = new Flow()
    flow.grid.cell = new Cell(0.1, 0.1, 10.0)
    val reader = new FlowXmlReader(flow)
    val xml = new XMLEventReader(Source.fromString(flowXml))
    val readFlowNodes = PrivateMethod[Vector[FlowPolygon]]('readXmlElements)
    val elements = reader invokePrivate readFlowNodes(xml)
    assert(elements.head.vertices.length == 4)
  }

  it should "construct okawana grid using half cell width" in {
    var flow = mock[Flow]
    val reader = new FlowXmlReader(flow)
    val constructArakawaAGrid = PrivateMethod[Vector[FlowPolygon]]('constructArakawaAGrid)
    var polygon = new FlowPolygon()
    reader invokePrivate constructArakawaAGrid(polygon, new GeoCoordinate(1.0, 2.0, 3.0), 0.5)
    assert(polygon.vertices.length == 4)
    assert(polygon.vertices.head.compare(new GeoCoordinate(0.5, 1.5)) == 0)
  }

  val flowXml: String =
    "<step timestep=\"day\" date=\"Mon Jan 01 00:00:00 EST 1996\">" +
      "<flow id=\"1\">" +
      "<depth value=\"5.01\" />" +
      "<salt value=\"35.24\" />" +
      "<temp value=\"14.85\" />" +
      "<velocity u=\"-0.18\" v=\"0.05\" w=\"7.274356E-7\"/>" +
      "<locus lat=\"-40.000534\" lon=\"142.00035\" />" +
      "</flow>" +
      "</step>"

  val twoFlowNodesXml: String =
    "<step timestep=\"day\" date=\"Mon Jan 01 00:00:00 EST 1996\">" +
      "<flow id=\"1\">" +
      "<depth value=\"5.01\" />" +
      "<salt value=\"35.24\" />" +
      "<temp value=\"14.85\" />" +
      "<velocity u=\"-0.18\" v=\"0.05\" w=\"7.274356E-7\"/>" +
      "<locus lat=\"-40.000534\" lon=\"142.00035\" />" +
      "</flow>" +
      "<flow id=\"2\">" +
      "<depth value=\"6.01\" />" +
      "<salt value=\"30.24\" />" +
      "<temp value=\"11.85\" />" +
      "<velocity u=\"-0.18\" v=\"0.05\" w=\"7.274356E-7\"/>" +
      "<locus lat=\"-40.000534\" lon=\"142.00035\" />" +
      "</flow>" +
      "</step>"

  val flowWithPropertiesXml: String =
    "<step timestep=\"day\" date=\"Mon Jan 01 00:00:00 EST 1996\">" +
      "<properties>" +
      " <dimensions longitude=\"201\" latitude=\"301\" depth=\"11\" />" +
      " <cellRange width=\"0.1\" depth=\"10.0\" />" +
      " <longitudeRange start=\"142.0\" end=\"162.0\" />" +
      " <latitudeRange start=\"-40.0\" end=\"-10.0\" />" +
      " <depthRange start=\"0.0\" end=\"100.0\" />" +
      "</properties>" +
      "<flow id=\"1\">" +
      "<depth value=\"5.01\" />" +
      "<salt value=\"35.24\" />" +
      "<temp value=\"14.85\" />" +
      "<velocity u=\"-0.18\" v=\"0.05\" w=\"7.274356E-7\"/>" +
      "<locus lat=\"-40.000534\" lon=\"142.00035\" />" +
      "</flow>" +
      "</step>"


}

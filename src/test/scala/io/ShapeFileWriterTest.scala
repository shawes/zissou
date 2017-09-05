package io

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class ShapeFileWriterTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The shape file writer" should "initialise" in {
    val shapeFileWriter = new GisShapeFile()
    assert(shapeFileWriter != null)
  }
}

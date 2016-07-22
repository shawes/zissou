package io

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class ShapeFileWriterTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The shape file writer" should "initialise" in {
    val shapeFileWriter = new GisShapeFile()
    assert(shapeFileWriter != null)
  }

  it should "throw an IllegalArgumentException when called with null paramaters" in {
    val shapeFileWriter = new GisShapeFile()
    intercept[IllegalArgumentException] {
      shapeFileWriter.write(null, null, null)
    }
  }



}

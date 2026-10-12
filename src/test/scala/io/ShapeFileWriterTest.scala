package io

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class ShapeFileWriterTest extends AnyFlatSpec with MockitoSugar with PrivateMethodTester {

  "The shape file writer" should "initialise" in {
    val shapeFileWriter = new GisShapeFile()
    assert(shapeFileWriter != null)
  }
}

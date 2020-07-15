package io

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._

class ShapeFileWriterTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  "The shape file writer" should "initialise" in {
    val shapeFileWriter = new GisShapeFile()
    assert(shapeFileWriter != null)
  }
}

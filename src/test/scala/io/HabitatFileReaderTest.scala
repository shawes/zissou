package io

import org.scalatest.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

/**
  * Created by steve on 25/01/2016.
  */
class HabitatFileReaderTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The habitat file writer" should "initialise" in {
    val habitatFileReader = new GisShapeFile()
    habitatFileReader should not be null
  }


}

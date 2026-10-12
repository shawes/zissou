package io

import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Created by steve on 25/01/2016.
  */
class HabitatFileReaderTest extends AnyFlatSpec with MockitoSugar with PrivateMethodTester {

  "The habitat file writer" should "initialise" in {
    val habitatFileReader = new GisShapeFile()
    habitatFileReader should not be null
  }


}

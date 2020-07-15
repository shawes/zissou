package io

import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._

/**
  * Created by steve on 25/01/2016.
  */
class HabitatFileReaderTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester
    with Matchers {

  "The habitat file writer" should "initialise" in {
    val habitatFileReader = new GisShapeFile()
    habitatFileReader should not be null
  }

}

package io

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers._

/**
  * Created by steve on 25/01/2016.
  */
class HabitatFileReaderTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The habitat file writer" should "initialise" in {
    val habitatFileReader = new HabitatFileReader()
    habitatFileReader should not be null
  }


}

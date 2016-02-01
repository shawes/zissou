package io

import java.io.File

import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

/**
  * Created by steve on 27/01/2016.
  */
class LarvalFileWriterTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The larval file writer" should "initialise with nulls" in {
    val larvalFileWriter = new LarvaeFileWriter(null, null)
    larvalFileWriter should not be null
  }

  it should "initialise with an empty list" in {
    val mockFile = mock[File]
    val larvalFileWriter = new LarvaeFileWriter(Nil, mockFile)
    larvalFileWriter should not be null
  }

  it should "have the following column headers" in {
    val headers = "id,born,age,stage,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"
    val mockFile = mock[File]
    val larvalFileWriter = new LarvaeFileWriter(Nil, mockFile)
    val result = larvalFileWriter.columnHeaders
    result should equal(headers)
  }

}

package io

import java.io.File

import biology.Larva
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.mockito.Mockito
import org.scalatest.Matchers._

/**
  * Created by steve on 27/01/2016.
  */
class LarvalFileWriterTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  val mockFile = mock[File]


  "The larval file writer" should "initialise with nulls" in {
    val larvalFileWriter = new LarvaeFileWriter(null,null)
    larvalFileWriter should not be null
  }

  it should "initialise with an empty list" in {
    val larvalFileWriter = new LarvaeFileWriter(Nil, mockFile)
    larvalFileWriter should not be null
  }

  it should "have the following column headers" in {
    val headers = "id,born,age,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"
    val larvalFileWriter = new LarvaeFileWriter(Nil, mockFile)
    val result = larvalFileWriter.columnHeaders
    result should equal (headers)
  }

  it should "have the following column headers" in {
    val headers = "id,born,age,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"
    val larvalFileWriter = new LarvaeFileWriter(Nil, mockFile)
    val result = larvalFileWriter.columnHeaders
    result should equal (headers)
  }







}

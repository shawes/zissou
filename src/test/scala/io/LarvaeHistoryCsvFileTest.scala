package io

import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._

/**
  * Created by steve on 27/01/2016.
  */
class LarvaeHistoryCsvFileTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester
    with Matchers {

  "The larval file writer" should "initialise with nulls" in {
    val larvalHistoryCsvFile = new LarvaeHistoryCsvFile(null, null, 0)
    larvalHistoryCsvFile should not be null
  }

  it should "initialise with an empty list" in {
    //val mockFile = mock[File]
    val larvalHistoryCsvFile = new LarvaeHistoryCsvFile(Array(), "", 0)
    larvalHistoryCsvFile should not be null
  }

  it should "have the following column headers" in {
    val headers =
      "id,born,age,stage,pld,birthplace,state,habitat-id,latitude,longitude,depth"
    //val mockFile = mock[File]
    val larvalHistoryCsvFile = new LarvaeHistoryCsvFile(Array(), "", 0)
    val result = larvalHistoryCsvFile.columnHeaders
    result should equal(headers)
  }

}

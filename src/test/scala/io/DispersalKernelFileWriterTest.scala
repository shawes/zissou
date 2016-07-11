package io

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class DispersalKernelFileWriterTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The dispersal kernel file writer" should "initialise" in {
    val dispersalKernelReader = new DispersalKernelFile(null, null)
    assert(dispersalKernelReader != null)
  }
  //
  //  it should "write a csv file" in {
  //    // create the mock
  //    val mockFileWriter = mock[FileWriter]
  //
  //
  //  }

  it should "have the correct column headers for the csv file" in {
    val ColumnHeaders = "id,born,birth_place,age,recruited,reef_id,reef_type"
    val dispersalKernelReader = new DispersalKernelFile(null, null)
    val columns = PrivateMethod[String]('columnHeaders)
    val headers = dispersalKernelReader invokePrivate columns()
    assert(headers == ColumnHeaders)
  }

}

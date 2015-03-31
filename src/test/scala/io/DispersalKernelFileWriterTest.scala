package io

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class DispersalKernelFileWriterTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The dispersal kernel file writer" should "initialise" in {
    val dispersalKernelReader = new DispersalKernelFileWriter(null, null)
    assert(dispersalKernelReader != null)
  }
  //
  //  it should "write a csv file" in {
  //    // create the mock
  //    val mockFileWriter = mock[FileWriter]
  //
  //
  //  }

}

package io

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}


class ConnectivityMatrixTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The connectivity matrix file writer" should "initialise" in {
    val ConnectivityMatrix = new ConnectivityMatrix(null, null)
    assert(ConnectivityMatrix != null)
  }

  it should "have the correct column headers for the csv file" in {
    val ColumnHeaders = "id,born,region,source,age,recruited,settle"
    val ConnectivityMatrix = new ConnectivityMatrix(null, null)
    val columns = PrivateMethod[String]('columnHeaders)
    val headers = ConnectivityMatrix invokePrivate columns()
    assert(headers == ColumnHeaders)
  }

}

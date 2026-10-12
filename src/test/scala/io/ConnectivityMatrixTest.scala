package io

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class ConnectivityMatrixTest
    extends AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  "The connectivity matrix file writer" should "initialise" in {
    val ConnectivityMatrix = new ConnectivityMatrix(null, null)
    assert(ConnectivityMatrix != null)
  }

  it should "have the correct column headers for the csv file" in {
    val ColumnHeaders = "id,born,region,source,age,recruited-date,settle-id"
    val ConnectivityMatrix = new ConnectivityMatrix(null, null)
    val columns = PrivateMethod[String](Symbol("columnHeaders"))
    val headers = ConnectivityMatrix invokePrivate columns()
    assert(headers == ColumnHeaders)
  }

}

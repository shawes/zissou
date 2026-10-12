package physical

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class GeoCoordinateTest extends AnyFlatSpec with MockitoSugar with PrivateMethodTester {

  "A geocoordinate" should "not be null" in {
    val coordinate = new GeoCoordinate(1,2,3)
    assert(coordinate != null)
  }

  it should "store the latitude as a double" in {
    val coordinate = new GeoCoordinate(1,2,3)
    assert(coordinate.latitude == 1.0)
  }

  it should "store the longitude as a double" in {
    val coordinate = new GeoCoordinate(1,2,3)
    assert(coordinate.longitude == 2.0)
  }

  it should "store the depth as a double" in {
    val coordinate = new GeoCoordinate(1,2,3)
    assert(coordinate.depth == 3.0)
  }

  it should "initialise with only lat and long" in {
    val coordinate = new GeoCoordinate(1,2)
    assert(coordinate.latitude == 1.0)
    assert(coordinate.longitude == 2.0)
    assert(coordinate.depth == 0)
  }

  it should "initialise with no parameters" in {
    val coordinate = new GeoCoordinate()
    assert(coordinate.latitude == 0)
    assert(coordinate.longitude == 0)
    assert(coordinate.depth == 0)
  }

  it should "not be valid if lat/lon have values" in {
    val coordinate = new GeoCoordinate(1,2,3)
    assert(coordinate.isValid)
  }

  it should "be undefined of lat is a Nan" in {
    val coordinate = new GeoCoordinate(Double.NaN,2,3)
    assert(coordinate.isValid == false)
  }

  it should "be undefined of lon is a Nan" in {
    val coordinate = new GeoCoordinate(1,Double.NaN,3)
    assert(coordinate.isValid == false)
  }

  it should "pretty print the coordinate" in {
    val coordinate = new GeoCoordinate(1,2,3)
    val printText = "lat=1.00000, lon=2.00000, depth=3.0"
    assert(coordinate.toString == printText)
  }

}

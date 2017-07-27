package physical

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class GeoCoordinateTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

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

  it should "know they are equal" in {
    val coordinate1 = new GeoCoordinate(1,2,3)
    val coordinate2 = new GeoCoordinate(1,2,3)
    assert(coordinate1.compare(coordinate2) == 0)
  }

  it should "know they are less than" in {
    val coordinate1 = new GeoCoordinate(1,2,3)
    val coordinate2 = new GeoCoordinate(3,2,1)
    assert(coordinate1.compare(coordinate2) < 0)
  }

  it should "know they are greater than" in {
    val coordinate1 = new GeoCoordinate(1,2,3)
    val coordinate2 = new GeoCoordinate(-3,2,1)
    assert(coordinate1.compare(coordinate2) > 0)
  }


}

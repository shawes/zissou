package biology

import org.scalatest.FlatSpec
import physical.GeoCoordinate

class BirthplaceTest extends FlatSpec {

  val birthplace = new Birthplace("hobart", 30, new GeoCoordinate(1, 2))

  "A birthplace" should "not be null upon construction" in {
    assert(birthplace != null)
  }

  it should "have the name passed to the constructor" in {
    assert(birthplace.name == "hobart")
  }
  
  it should "have the reef passed to the constructor" in {
    assert(birthplace.reef == 30)
  }

  it should "have the coordinate passed to the constructor" in {
    assert(birthplace.location.latitude == 1)
    assert(birthplace.location.longitude == 2)
  }


}

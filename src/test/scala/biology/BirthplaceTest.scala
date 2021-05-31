package biology

//import org.scalatest._
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import physical.GeoCoordinate

class BirthplaceTest extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  test("birthplace construction") {
    forAll { (name: String, reefId: Int, lat: Int, lon: Int) =>
      val birthplace = new Birthplace(name, reefId, new GeoCoordinate(lat, lon))
      assert(birthplace.name == name)
      assert(birthplace.reef == reefId)
      assert(birthplace.location.latitude == lat)
      assert(birthplace.location.longitude == lon)
    }
  }

}

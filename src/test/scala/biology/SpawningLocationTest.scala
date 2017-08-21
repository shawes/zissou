package biology

import com.github.nscala_time.time.Imports._
import org.scalatest.FlatSpec
import physical.GeoCoordinate

class SpawningLocationTest extends FlatSpec {

  val spawningLocation = new SpawningLocation("lizard island", 1000, new GeoCoordinate(1, 2),  DateTime.now.minusDays(5) to DateTime.now.plusDays(1), 5)

  "A spawning location" should "be able to construct" in {
    assert(spawningLocation != null)
  }

  it should "return true if it can spawn" in {
    val can = spawningLocation.canSpawn(DateTime.now)
    assert(can)
  }

  it should "return false if it cannot spawn" in {
    val cannot = !spawningLocation.canSpawn(DateTime.now.plusDays(10))
    assert(cannot)
  }


}

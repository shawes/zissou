package biology

import com.github.nscala_time.time.Imports._
import locals.PelagicLarvaeState
import org.scalatest.FlatSpec

class ReefFishTest extends FlatSpec {
  val pld: Int = 30

  "A fish" should "start at age zero" in {
    val fish = new ReefFish(0, pld, 0, null, null)
    assert(fish.age == 0)
  }

  it should "have a pelagic larval duration" in {
    val fish = new ReefFish(0, pld, 0, null, null)
    assert(fish.pelagicLarvalDuration == pld)
  }

  it should "know when it was born" in {
    val fish = new ReefFish(0, pld, 0, null, null)
    val now = DateTime.now
    assert(fish.birthday.dayOfMonth() == now.dayOfMonth())
    assert(fish.birthday.dayOfWeek() == now.dayOfWeek())
    assert(fish.birthday.year() == now.year())
  }

  it should "be pelagic when it is born" in {
    val fish = new ReefFish(1, 2, 3, null, PelagicLarvaeState.Pelagic)
    assert(fish.state == PelagicLarvaeState.Pelagic)
  }

  it should "have no history when its born" in {
    val fish = new ReefFish(1, 2, 3, null, null)
    assert(fish.history.isEmpty)
  }


}
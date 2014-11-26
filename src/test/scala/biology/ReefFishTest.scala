package biology

import com.github.nscala_time.time.Imports._
import org.scalatest.FlatSpec

class ReefFishTest extends FlatSpec {
  val pld: Int = 30
  "A fish" should "start at age zero" in {
    val fish = new ReefFish(0, pld, 0)
    assert(fish.age == 0)
  }

  it should "have a pelagic larval duration" in {
    val fish = new ReefFish(0, pld, 0)
    assert(fish.pelagicLarvalDuration == pld)
  }

  it should "know when it was born" in {
    val fish = new ReefFish(0, pld, 0)
    assert(fish.birthday.date.compareTo(DateTime.now.date) == 0)
  }
}
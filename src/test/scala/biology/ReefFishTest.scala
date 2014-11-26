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
    val now =  DateTime.now
    assert(fish.birthday.dayOfMonth()== now.dayOfMonth())
    assert(fish.birthday.dayOfWeek()== now.dayOfWeek())
    assert(fish.birthday.year()== now.year())
  }
}
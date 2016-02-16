package biology

import com.github.nscala_time.time.Imports._
import locals.{HabitatType, PelagicLarvaeState}
import maths.{ContinuousRange, RandomNumberGenerator}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import physical.GeoCoordinate
import physical.habitat.GeometryAdaptor

class ReefFishTest extends FlatSpec with MockitoSugar with PrivateMethodTester {
  val pld: Int = 30
  val id: Int = 1
  val maximumLifespan = 50
  val ontogeny = new Ontogeny(10, 15, 20)
  val birthplace = new Birthplace("Adelaide", new GeoCoordinate(10, 150, 15))
  val pelagicState = PelagicLarvaeState.Pelagic
  val verticalMigration: List[VerticalMigrationProbability] =
    List(
      new VerticalMigrationProbability(new ContinuousRange(1, 25, false), 0.9, 0.6, 0.3, 0.2),
      new VerticalMigrationProbability(new ContinuousRange(25, 50, false), 0.1, 0.3, 0.5, 0.35),
      new VerticalMigrationProbability(new ContinuousRange(50, 75, false), 0, 0.1, 0.15, 0.40),
      new VerticalMigrationProbability(new ContinuousRange(75, 100, false), 0, 0, 0.05, 0.05)
    )

  "A fish" should "start at age zero" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(fish.age == 0)
  }

  it should "have a pelagic larval duration" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(fish.pelagicLarvalDuration == pld)
  }

  it should "know when it has attained pelagic larval duration" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(!fish.hasBeenPelagicTooLong)
    fish.growOlder(29) // below PLD
    assert(!fish.hasBeenPelagicTooLong)
    fish.growOlder(1) // equals PLD
    assert(fish.hasBeenPelagicTooLong)
    fish.growOlder(1) // greater than PLD
    assert(fish.hasBeenPelagicTooLong)
  }

  /*  it should "know when it is in the competency window" in {
      val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
        ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
      assert(fish.inCompetencyWindow)
      fish.growOlder(29) // below PLD
      assert(fish.inCompetencyWindow)
      fish.growOlder(1) // equals PLD
      assert(!fish.inCompetencyWindow)
      fish.growOlder(1) // greater than PLD
      assert(!fish.inCompetencyWindow)
    }*/

  it should "know when it has reached max life span" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(!fish.isTooOld)
    fish.growOlder(49) // below max
    assert(!fish.isTooOld)
    fish.growOlder(1) // equals max
    assert(fish.isTooOld)
    fish.growOlder(1) // greater than max
    assert(fish.isTooOld)
  }

  it should "know when it was born" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val now = DateTime.now
    assert(fish.birthday.dayOfMonth() == now.dayOfMonth())
    assert(fish.birthday.dayOfWeek() == now.dayOfWeek())
    assert(fish.birthday.year() == now.year())
  }

  it should "be pelagic when it is born" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(fish.state == PelagicLarvaeState.Pelagic)
  }

  it should "have no history when its born" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(fish.history.isEmpty)
  }

  it should "not move when there is an invalid position" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val invalidPoint = new GeoCoordinate(Double.NaN, Double.NaN, 0)
    fish.move(invalidPoint)
    assert(fish.position == birthplace.location)
  }

  it should "move when there is an valid position" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val validPoint = new GeoCoordinate(2, 2, 2)
    fish.move(validPoint)
    assert(fish.position != birthplace.location)
    assert(fish.position == validPoint)
  }

  it should "have saved state once its moved" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val validPoint = new GeoCoordinate(2, 2, 2)
    fish.move(validPoint)
    assert(fish.history.nonEmpty)
    assert(fish.isPelagic)
  }

  it should "calls vertical migration appropriately" in {
    val mockVertical = mock[VerticalMigration]
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, mockVertical)
    val random = new RandomNumberGenerator(100)
    fish.getOntogeneticVerticalMigrationDepth(random)
    verify(mockVertical).getDepth(fish.getOntogeny, random)
  }


  it should "grow older" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val expected = fish.age + 3000
    fish.growOlder(3000)
    assert(fish.age == expected)
  }

  it should "know when it's pelagic" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(fish.isPelagic)
    fish.kill()
    assert(!fish.isPelagic)
  }

  it should "know when it's dead" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    assert(!fish.isDead)
    fish.kill()
    assert(fish.isDead)
  }

  it should "settle when asked" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val reef = new GeometryAdaptor(null, 116, HabitatType.Reef)
    val settleTime = DateTime.now
    fish.settle(reef, settleTime)
    assert(fish.polygon == reef)
    assert(fish.settlementDate == settleTime)
    assert(fish.state == PelagicLarvaeState.Settled)
  }

  it should "know when it's settled" in {
    val fish = new ReefFish(id, pld, maximumLifespan, birthplace, DateTime.now,
      ontogeny, new VerticalMigration(List.empty[VerticalMigrationProbability]))
    val reef = new GeometryAdaptor(null, 116, HabitatType.Reef)
    val settleTime = DateTime.now
    assert(!fish.isSettled)
    fish.settle(reef, settleTime)
    assert(fish.isSettled)
  }

}
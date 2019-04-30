/* package biology.fish

import com.github.nscala_time.time.Imports._
import locals._
import maths.ContinuousRange
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import physical.GeoCoordinate
import physical.habitat.GeometryAdaptor
import biology._

class FishTest extends FlatSpec with MockitoSugar with PrivateMethodTester {
  val pld: Int = 30
  val id: Int = 1
  val maximumLifespan = 50
  val ontogeny = new OntogenyFish(10, 15, 20)
  val birthplace =
    new Birthplace("Adelaide", 101, new GeoCoordinate(10, 150, 15))
  val pelagicState = Pelagic
  val swimming = new Swimming(SwimmingAbility.Directed, 1, 2, 3, false)
  val ontogeneticVerticalMigration
      : List[VerticalMigrationOntogeneticProbability] =
    List(
      new VerticalMigrationOntogeneticProbability(
        new ContinuousRange(1, 25, false),
        0.9,
        0.6,
        0.3,
        0.2
      ),
      new VerticalMigrationOntogeneticProbability(
        new ContinuousRange(25, 50, false),
        0.1,
        0.3,
        0.5,
        0.35
      ),
      new VerticalMigrationOntogeneticProbability(
        new ContinuousRange(50, 75, false),
        0,
        0.1,
        0.15,
        0.40
      ),
      new VerticalMigrationOntogeneticProbability(
        new ContinuousRange(75, 100, false),
        0,
        0,
        0.05,
        0.05
      )
    )

  "A fish" should "start at age zero" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(fish.age == 0)
  }

  it should "have a pelagic larval duration" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(fish.pelagicLarvalDuration == pld)
  }

  /*  it should "know when it has attained pelagic larval duration" in {
      val fish = new Fish(id, pld, maximumLifespan, birthplace, LocalDateTime.now,
        ontogeny, new VerticalMigrationOntogenetic(new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]))
      assert(!fish.hasBeenPelagicTooLong)
      fish.growOlder(29) // below PLD
      assert(!fish.hasBeenPelagicTooLong)
      fish.growOlder(1) // equals PLD
      assert(fish.hasBeenPelagicTooLong)
      fish.growOlder(1) // greater than PLD
      assert(fish.hasBeenPelagicTooLong)
    }*/

  /*  it should "know when it is in the competency window" in {
      val fish = new Fish(id, pld, maximumLifespan, birthplace, LocalDateTime.now,
        ontogeny, new VerticalMigrationOntogenetic(new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]))
      assert(fish.inCompetencyWindow)
      fish.growOlder(29) // below PLD
      assert(fish.inCompetencyWindow)
      fish.growOlder(1) // equals PLD
      assert(!fish.inCompetencyWindow)
      fish.growOlder(1) // greater than PLD
      assert(!fish.inCompetencyWindow)
    }

  it should "know when it has reached max life span" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(!fish.isTooOld)
    fish.growOlder(49) // below max
    assert(!fish.isTooOld)
    fish.growOlder(1) // equals max
    assert(!fish.isTooOld)
    fish.growOlder(1) // greater than max
    assert(fish.isTooOld, "age is " + fish.age)
  }

  it should "know when it was born" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    val now = LocalDateTime.now
    assert(fish.birthday.dayOfMonth() == now.dayOfMonth())
    assert(fish.birthday.dayOfWeek() == now.dayOfWeek())
    assert(fish.birthday.year() == now.year())
  }

  it should "be pelagic when it is born" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(fish.isPelagic)
  }

  it should "have no history when its born" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(fish.history.isEmpty)
  }

  // it should "not move when there is an invalid position" in {
  //   val fish = new Fish(id, pld, maximumLifespan, birthplace, LocalDateTime.now,
  //     ontogeny, swimming, new VerticalMigrationOntogenetic(List.empty[VerticalMigrationOntogeneticProbability]), new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]))
  //   val invalidPoint = new GeoCoordinate(Double.NaN, Double.NaN, 0)
  //   fish.move(invalidPoint)
  //   assert(fish.position == birthplace.location)
  // }

  it should "move when there is an valid position" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    val validPoint = new GeoCoordinate(2, 2, 2)
    fish.move(validPoint)
    assert(fish.position != birthplace.location)
    assert(fish.position == validPoint)
  }

  it should "have saved state once its moved" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    val validPoint = new GeoCoordinate(2, 2, 2)
    fish.move(validPoint)
    assert(fish.history.nonEmpty)
    assert(fish.isPelagic)
  }

  it should "calls ontogenetic vertical migration appropriately" in {
    val mockOntogeneticVerticalMigration = mock[VerticalMigrationOntogenetic]
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      mockOntogeneticVerticalMigration,
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    fish.ontogeneticVerticallyMigrate
    verify(mockOntogeneticVerticalMigration).getDepth(fish.getOntogeny, 15)
  }

  it should "calls diel vertical migration appropriately" in {
    val mockDielVerticalMigration = mock[VerticalMigrationDiel]
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      mockDielVerticalMigration,
      0
    )
    fish.dielVerticallyMigrate(DielVerticalMigrationType.Day)
    verify(mockDielVerticalMigration).getDepth(DielVerticalMigrationType.Day)
  }

  it should "grow older" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    val expected = fish.age + 3000
    fish.growOlder(3000)
    assert(fish.age == expected)
  }

  it should "know when it's pelagic" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(fish.isPelagic)
    fish.kill()
    assert(!fish.isPelagic)
  }

  it should "know when it's dead" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    assert(!fish.isDead)
    fish.kill()
    assert(fish.isDead)
  }

  it should "settle when asked" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    //val reef = new GeometryAdaptor(null, 116, HabitatType.Reef)
    val settleTime = LocalDateTime.now
    fish.settle(116, settleTime)
    assert(fish.polygon == 116)
    assert(fish.settlementDate == settleTime)
    assert(fish.isSettled)
  }

  it should "know when it's settled" in {
    val fish = new Fish(
      id,
      pld,
      maximumLifespan,
      birthplace,
      LocalDateTime.now,
      ontogeny,
      swimming,
      new VerticalMigrationOntogenetic(
        StageMigration,
        List.empty[VerticalMigrationOntogeneticProbability]
      ),
      new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability]),
      0
    )
    //val reef = new GeometryAdaptor(null, 116, HabitatType.Reef)
    val settleTime = LocalDateTime.now
    assert(!fish.isSettled)
    fish.settle(116, settleTime)
    assert(fish.isSettled)
  }

}
 */
 */

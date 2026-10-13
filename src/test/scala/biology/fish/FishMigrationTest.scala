package biology.fish

import com.github.nscala_time.time.Imports._
import biology.Birthplace
import biology.swimming.{DielMigration, OntogeneticMigration, OntogeneticMigrationVariables}
import io.config.OntogeneticMigrationConfig
import locals._
import org.scalatest.flatspec.AnyFlatSpec
import physical.GeoCoordinate

class FishMigrationTest extends AnyFlatSpec {

  // deep by day (76 to 100 m), shallow by night (0 to 25 m)
  private val diel = new DielMigration(List(25, 50, 75, 100), List(0, 0, 0, 1), List(1, 0, 0, 0))
  // between 31 and 60 m at every stage
  private val ovm = new OntogeneticMigration(
    OntogeneticMigrationConfig("stage", List(30, 60), List(0, 1), List(0, 1), List(0, 1), List(0, 1))
  )

  private def fish(ovm: Option[OntogeneticMigration], diel: Option[DielMigration]): Fish =
    new Fish("a", 86400 * 10, 86400 * 10, new Birthplace("reef", 101, new GeoCoordinate(-28.0, 153.5, 5.0)),
      new LocalDateTime(2002, 1, 5, 0, 0), 86400, 86400 * 2, 86400 * 3, ovm, diel, None, 86400)

  private val developed = new OntogeneticMigrationVariables(true, false)

  private def depth(larva: Fish) = larva.position.depth
  private def isDayDepth(d: Double) = d >= 76 && d <= 100
  private def isOntogeneticDepth(d: Double) = d >= 31 && d <= 60

  "A larva" should "take on a stage on its first step, the one it is born in, and then on each change of stage" in {
    val larva = fish(Some(ovm), None) // preflexion at 1 day, flexion at 2, postflexion at 3
    val steps = (1 to 48).map(_ => larva.incrementAge(7200)) // four days of 2 hour steps
    // the first step, then the steps on which it turns 1, 2 and 3 days old
    assert(steps.zipWithIndex.collect { case (true, i) => i + 1 } == Seq(1, 12, 24, 36))
  }

  "A larva that migrates daily" should "migrate once at dawn and once at dusk, however many steps they last" in {
    val larva = fish(None, Some(diel))
    larva.dielMigrate(Day)
    val day = depth(larva)
    larva.dielMigrate(Day)
    assert(depth(larva) == day)
    larva.dielMigrate(Night)
    val night = depth(larva)
    assert(night <= 25)
    larva.dielMigrate(Night)
    assert(depth(larva) == night)
  }

  "A larva with both migrations" should "go back at dusk to the depth it had before dawn, when dawn lasts two steps" in {
    val larva = fish(Some(ovm), Some(diel))
    larva.ovmMigrate(developed)
    val before = depth(larva)
    assert(isOntogeneticDepth(before))
    larva.dielMigrate(Day)
    larva.dielMigrate(Day)
    assert(isDayDepth(depth(larva)))
    larva.dielMigrate(Night)
    larva.dielMigrate(Night)
    assert(depth(larva) == before)
  }

  it should "stay at its day depth when it develops by day, and go to its new depth at dusk" in {
    val larva = fish(Some(ovm), Some(diel))
    larva.dielMigrate(Day)
    val day = depth(larva)
    larva.ovmMigrate(developed)
    assert(depth(larva) == day)
    larva.dielMigrate(Night)
    assert(isOntogeneticDepth(depth(larva)))
  }

  it should "take its ontogenetic depth when it develops by night" in {
    val larva = fish(Some(ovm), Some(diel))
    larva.dielMigrate(Night)
    larva.ovmMigrate(developed)
    assert(isOntogeneticDepth(depth(larva)))
  }
}

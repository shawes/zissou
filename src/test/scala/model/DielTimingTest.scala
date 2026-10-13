package model

import com.github.nscala_time.time.Imports._
import org.scalatest.flatspec.AnyFlatSpec
import tools.ModelFixture

class DielTimingTest extends AnyFlatSpec {

  private val inputs = ModelFixture.inputs()

  // Deep by day (76 to 100 m) and shallow by night (0 to 25 m), always
  private val dielMigrant = ModelFixture.driftingLarva(
    pld = 4,
    nonSettlementPeriod = 4, // so that none settle, and all live out their four days
    extra = """  dielProbabilities:
              |    depths: [25, 50, 75, 100]
              |    day: [0, 0, 0, 1]
              |    night: [1, 0, 0, 0]
              |""".stripMargin
  )

  "A larva that migrates daily" should "be deep in the middle of the local day and shallow in the middle of the local night" in {
    // released at 153.5 east, ten hours ahead of UTC on the clock that times the sun
    val model = ModelFixture.run(ModelFixture.yaml(inputs, dielMigrant, numberOfLarvae = 20))
    val larvae = model.biology.stationaryLarvae
    assert(larvae.size == 20)
    var day, night = 0
    // released at 10:00 local, a larva keeps the depth it was spawned at until its first dusk
    for (larva <- larvae; capsule <- larva.history if capsule.age > 86400) {
      // a capsule holds where the larva was at the start of the step that took it to this age
      val clock = larva.birthday.plusSeconds(capsule.age - 7200)
      val localHour = clock.plusHours(10).getHourOfDay
      val depth = capsule.position.depth
      if (localHour >= 10 && localHour <= 14) {
        assert(depth > 75, s"$depth m at $localHour:00 local")
        day += 1
      } else if (localHour >= 22 || localHour <= 2) {
        assert(depth <= 25, s"$depth m at $localHour:00 local")
        night += 1
      }
    }
    assert(day > 100 && night > 100)
  }
}

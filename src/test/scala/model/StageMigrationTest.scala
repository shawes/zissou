package model

import org.scalatest.flatspec.AnyFlatSpec
import tools.ModelFixture

class StageMigrationTest extends AnyFlatSpec {

  private val inputs = ModelFixture.inputs()

  // 31 to 60 m in its first stage, and 0 to 30 m from preflexion on
  private def stageMigrant(ontogeny: (Int, Int, Int)) = ModelFixture.driftingLarva(
    pld = 5,
    nonSettlementPeriod = 5,
    ontogeny = ontogeny,
    extra = """  ovmProbabilities:
              |    implementation: stage
              |    depths: [30, 60]
              |    hatching: [0, 1]
              |    preflexion: [1, 0]
              |    flexion: [1, 0]
              |    postflexion: [1, 0]
              |""".stripMargin
  )

  private def isShallow(depth: Double) = depth <= 30
  private def isDeep(depth: Double) = depth > 30 && depth <= 60

  "A larva that migrates by stage" should "go to the depths of the stage it is born in on its first step" in {
    val model = ModelFixture.run(ModelFixture.yaml(inputs, stageMigrant((2, 3, 4)), numberOfLarvae = 20))
    val larvae = model.biology.stationaryLarvae
    assert(larvae.size == 20)
    var early, late = 0
    for (larva <- larvae; capsule <- larva.history) {
      val fish = larva.asInstanceOf[biology.fish.Fish]
      // a capsule holds where the larva was at the start of the step that took it to its age,
      // after any migration on the step before
      if (capsule.age >= 2 * 7200 && capsule.age <= fish.preflexion) {
        assert(isDeep(capsule.position.depth), s"${capsule.position.depth} m at ${capsule.age / 3600} hours, before preflexion")
        early += 1
      } else if (capsule.age > fish.preflexion + 7200) {
        assert(isShallow(capsule.position.depth), s"${capsule.position.depth} m at ${capsule.age / 3600} hours")
        late += 1
      }
    }
    assert(early > 200 && late > 200)
  }

  it should "go to the depths of preflexion on its first step when it is born in preflexion" in {
    // with no egg stage, a larva is born in preflexion
    val model = ModelFixture.run(ModelFixture.yaml(inputs, stageMigrant((0, 3, 4)), numberOfLarvae = 20))
    // before flexion, its first change of stage
    val depths = for {
      larva <- model.biology.stationaryLarvae
      capsule <- larva.history if capsule.age >= 2 * 7200 && capsule.age <= larva.asInstanceOf[biology.fish.Fish].flexion
    } yield capsule.position.depth
    assert(depths.size > 300)
    assert(depths.forall(isShallow))
    // and not stay where it was spawned, 0 to 5 m
    assert(depths.exists(_ > 5))
  }
}

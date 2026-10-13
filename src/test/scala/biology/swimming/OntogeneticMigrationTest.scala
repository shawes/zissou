package biology.swimming

import maths.ContinuousRange
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class OntogeneticMigrationTest
    extends AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  /*   "A fish ontogentic vertical migration probabilty" should "be able to construct the " in {
    val result = new OntogeneticMigrationProbability(
      new ContinuousRange(50, 100, true),
      100,
      200,
      300,
      400
    )
    assert(result.hatching == 100)
    assert(result.preflexion == 200)
    assert(result.flexion == 300)
    assert(result.postflexion == 400)
    assert(result.depth.start == 50)
    assert(result.depth.end == 100)
    assert(result.depth.isInclusive)
  } */

  private def migration(implementation: String) = new OntogeneticMigration(
    io.config.OntogeneticMigrationConfig(implementation, List(5, 50, 100), List(0, 0, 0), List(0.4, 0.5, 0.1), List(0, 1, 0), List(0, 0, 1))
  )

  "A stage whose probabilities are all 0" should "leave the larva where it is" in {
    for (implementation <- Seq("stage", "daily", "timestep"); depth <- Seq(2.5, 30.0, 80.0)) {
      assert(migration(implementation)(locals.Hatching, depth) == depth, s"$implementation from $depth m")
    }
  }

  "A stage with probabilities" should "take the larva to a depth drawn from them" in {
    for (implementation <- Seq("stage", "daily")) {
      val depth = migration(implementation)(locals.Postflexion, 2.5)
      assert(depth >= 51 && depth <= 100, s"$implementation: $depth m")
    }
  }
}

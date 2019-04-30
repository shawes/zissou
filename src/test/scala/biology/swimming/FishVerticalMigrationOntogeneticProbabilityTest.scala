package biology.swimming

import maths.ContinuousRange
import org.scalatest.FlatSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class FishVerticalMigrationOntogeneticProbabilityTest
    extends FlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  "A fish ontogentic vertical migration probabilty" should "be able to construct the " in {
    val result = new VerticalMigrationOntogeneticProbability(
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
  }

}

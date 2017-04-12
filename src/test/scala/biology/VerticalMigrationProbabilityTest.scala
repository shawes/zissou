package biology

import maths.ContinuousRange
import org.scalatest.FlatSpec

class VerticalMigrationProbabilityTest extends FlatSpec {

  "Vertical migration" should "be able to construct the " in {
    val result = new VerticalMigrationProbability(new ContinuousRange(50, 100, true), 100, 200, 300, 400)
    assert(result.hatching == 100)
    assert(result.preFlexion == 200)
    assert(result.flexion == 300)
    assert(result.postFlexion == 400)
    assert(result.depth.start == 50)
    assert(result.depth.end == 100)
    assert(result.depth.isInclusive)
  }

}

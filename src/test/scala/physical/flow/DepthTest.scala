package physical.flow

import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class DepthTest extends FlatSpec with MockitoSugar {

  "The depth constructor" should "initialise with default variables" in {
    val depth = new Depth()
    assert(!depth.average)
    assert(!depth.averageOverAllDepths)
    assert(depth.maximumDepthForAverage == 0)
    assert(depth.range != null)
  }


}

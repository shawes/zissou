package maths

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class ContinuousRangeTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  val rangeParams = new ContinuousRange(1, 10, true)
  val rangeNoParams = new ContinuousRange()

  "A continous range with params" should "not be null " in {
    assert(rangeParams != null)
  }

  it should "allow parameterless construction" in {
    assert(rangeNoParams != null)
  }

  it should "set start correctly for supplied parameters" in {
    assert(rangeParams.start == 1)
  }

  it should "set end correctly for supplied parameters" in {
    assert(rangeParams.end == 10)
  }

  it should "set isInclusive correctly for supplied parameters" in {
    assert(rangeParams.isInclusive)
  }

  it should "set start correctly with no parameters" in {
    assert(rangeNoParams.start == 0)
  }

  it should "set end correctly with no parameters" in {
    assert(rangeNoParams.end == 0)
  }

  it should "set isInclusive correctly with no parameters" in {
    assert(!rangeNoParams.isInclusive)
  }


}

package maths

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class ContinuousRangeTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  val start = 1.0
  val end = 10.0
  val rangeParams = new ContinuousRange(start, end, true)
  val rangeNotInclusive = new ContinuousRange(start, end, false)
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

  it should "contain a number in the range" in {
    assert(rangeParams.contains(end/2))
  }

  it should "not contain a number before the range start" in {
    assert(rangeParams.contains(start-1) == false)
  }

  it should "not contain a number after the range end" in {
    assert(rangeParams.contains(end+1) == false)
  }

  it should "contain the start in the range if inclusive" in {
    assert(rangeParams.contains(start))
  }

  it should "contain the end in the range if inclusive" in {
    assert(rangeParams.contains(end))
  }

  it should "not contain the start in the range if not inclusive" in {
    assert(rangeNotInclusive.contains(start) == false)
  }

  it should "not contain the end in the range if not inclusive" in {
    assert(rangeNotInclusive.contains(end) == false)
  }
}

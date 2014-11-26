package biology

import org.scalatest.FlatSpec

class MortalityTest extends FlatSpec {
  val mortality = new Mortality(2.2)

  "A mortality object" should "not be null upon construction" in {
    assert(mortality != null)
  }

  it should "calculate lambda correctly" in {
    val expected = scala.math.log(2) / (2.2 * 0.5)
    assert(mortality.lambda == expected)
  }

  it should "calculated the mortality rate given the step" in {
    val step = 5
    val rate = scala.math.exp(-1 * mortality.lambda * step)
    assert(mortality.calculateMortalityRate(step) == rate)
  }


}

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
    mortality.calculateMortalityRate(step)
    assert(mortality.getRate == rate)
  }

  it should "mortality decreased with age" in {
    val rate1 = scala.math.exp(-1 * mortality.lambda * 1)
    val rate2 = scala.math.exp(-1 * mortality.lambda * 2)
    val rate3 = scala.math.exp(-1 * mortality.lambda * 3)
    val rate4 = scala.math.exp(-1 * mortality.lambda * 4)
    val rate5 = scala.math.exp(-1 * mortality.lambda * 5)
    assert(rate1 > rate2)
    assert(rate2 > rate3)
    assert(rate3 > rate4)
    assert(rate4 > rate5)
  }


}

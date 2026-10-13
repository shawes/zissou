package biology

import org.scalatest.flatspec.AnyFlatSpec

class TemperatureMortalityTest extends AnyFlatSpec {

  private def hazard(rate: Double) = -math.log(1.0 - rate)

  "Mortality that depends on temperature" should "be the configured rate at the reference temperature" in {
    val m = new TemperatureMortality(0.26, 24.0, 2.0)
    assert(math.abs(m.rateAt(Some(24.0)) - 0.26) < 1e-12 && m.getRate == 0.26)
  }

  it should "double the hazard for every 10 degrees warmer, with a Q10 of 2, and halve it for every 10 cooler" in {
    val m = new TemperatureMortality(0.26, 24.0, 2.0)
    assert(math.abs(hazard(m.rateAt(Some(34.0))) - 2 * hazard(0.26)) < 1e-12)
    assert(math.abs(hazard(m.rateAt(Some(44.0))) - 4 * hazard(0.26)) < 1e-12)
    assert(math.abs(hazard(m.rateAt(Some(14.0))) - 0.5 * hazard(0.26)) < 1e-12)
    // 5 degrees is half a step: a factor of the square root of 2
    assert(math.abs(hazard(m.rateAt(Some(29.0))) - math.sqrt(2) * hazard(0.26)) < 1e-12)
  }

  it should "be a probability whatever the temperature, higher the warmer" in {
    val m = new TemperatureMortality(0.5, 20.0, 3.0)
    val rates = (-5 to 40 by 5).map(t => m.rateAt(Some(t.toDouble)))
    assert(rates.forall(r => r > 0 && r < 1))
    // a very hot sea kills every larva in a day: the rate saturates at 1, and never goes above it
    assert(m.rateAt(Some(80.0)) <= 1.0)
    assert(rates == rates.sorted && rates.distinct.size == rates.size)
    // the daily rate at 30 degrees, from the daily rate at 20: 1 - (1 - 0.5)^3
    assert(math.abs(m.rateAt(Some(30.0)) - 0.875) < 1e-12)
  }

  it should "use the reference rate where the temperature is not known" in {
    val m = new TemperatureMortality(0.26, 24.0, 2.0)
    assert(m.rateAt(None) == 0.26 && m.rateAt(Some(Double.NaN)) == 0.26)
  }

  it should "not change the rate at all with a Q10 of 1, or with a rate of 0" in {
    val flat = new TemperatureMortality(0.3, 20.0, 1.0)
    assert((0 to 40 by 10).forall(t => math.abs(flat.rateAt(Some(t.toDouble)) - 0.3) < 1e-12))
    assert(new TemperatureMortality(0.0, 20.0, 2.0).rateAt(Some(35.0)) == 0.0)
  }

  it should "refuse a rate of 1 or more, and a Q10 that is not above 0" in {
    intercept[IllegalArgumentException](new TemperatureMortality(1.0, 20.0, 2.0))
    intercept[IllegalArgumentException](new TemperatureMortality(0.2, 20.0, 0.0))
    intercept[IllegalArgumentException](new TemperatureMortality(0.2, 20.0, -2.0))
  }
}

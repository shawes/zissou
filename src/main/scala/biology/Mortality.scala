package biology

trait Mortality {
  def getRate: Double
}

class MortalityDecay(age : Double, pld: Double) extends Mortality {
  val lambda = scala.math.log(2) / (pld * 0.5)
  val rate = scala.math.exp(-1 * lambda * age)
  def getRate : Double = rate
}

class MortalityConstant(rate: Double) extends Mortality {
  def getRate : Double = rate
}

/**
  * Mortality that depends on temperature. At the reference temperature a larva dies each day with the probability
  * `referenceRate`; the hazard behind that, -ln(1 - referenceRate), is multiplied by q10 ^ ((T - reference) / 10) at
  * another temperature T, and the probability of dying in a day is 1 - exp(-hazard). With a q10 of 2 the hazard doubles
  * for every 10 degrees warmer and halves for every 10 degrees cooler. Where the temperature is not known the
  * reference rate is used.
  */
class TemperatureMortality(referenceRate: Double, referenceTemperature: Double, q10: Double) extends Mortality {
  require(referenceRate >= 0 && referenceRate < 1, s"The mortality rate at the reference temperature must be from 0 to below 1, not $referenceRate")
  require(q10 > 0 && !q10.isInfinite, s"The Q10 of mortality must be more than 0, not $q10")

  private val referenceHazard = -math.log(1.0 - referenceRate)

  /** The probability of dying in a day at the reference temperature. */
  def getRate: Double = referenceRate

  /** The probability of dying in a day at a temperature, in degrees C, or at the reference temperature where it is not known. */
  def rateAt(temperature: Option[Double]): Double =
    temperature.filterNot(_.isNaN) match {
      case None => referenceRate
      case Some(t) => 1.0 - math.exp(-referenceHazard * math.pow(q10, (t - referenceTemperature) / 10.0))
    }
}

package biology

class Mortality(pld: Double) {
  val lambda = scala.math.log(2) / (pld * 0.5)

  def calculateMortalityRate(step: Int): Double = scala.math.exp(-1 * lambda * step)
}

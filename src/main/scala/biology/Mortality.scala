package biology

class Mortality(pld: Double) {

  val lambda = scala.math.log(2) / (pld * 0.5)
  private var rate: Double = 0

  def calculateMortalityRate(step: Int): Unit = {
    rate = scala.math.exp(-1 * lambda * step)
  }

  def getRate = rate


}

package biology

trait Mortality {
  def getRate: Double
}

//TODO: Fix the mortality rate, it doesn't calculate correctly
class MortalityDecay(age : Double, pld: Double) extends Mortality {
  val lambda = scala.math.log(2) / (pld * 0.5)
  val rate = scala.math.exp(-1 * lambda * age)
  
  def getRate : Double = rate
}

class MortalityConstant(rate: Double) extends Mortality {
  def getRate : Double = rate
}

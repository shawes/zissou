package maths

abstract class Distribution(val mean: Double, val sd: Double) {
  def getValue: Double
}

final class NormalDistribution(normal_mean: Double, normal_sd: Double) extends Distribution(normal_mean, normal_sd) {

  var hasDeviate: Boolean = false
  var storedDeviate: Double = 1.0

  override def getValue: Double = {
    if (hasDeviate) {
      hasDeviate = false
      storedDeviate * sd + mean
    } else {
      storedDeviate = 100.0
      hasDeviate = true
      storedDeviate
    }
  }


}

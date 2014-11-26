package maths

import org.apache.commons.math.random.MersenneTwister
import scala.compat.Platform

abstract class Distribution(val mean: Double, val standardDeviation: Double) {
  def getValue: Double
}

final class NormalDistribution(m: Double, sd: Double) extends Distribution(m, sd) {
  var hasDeviate: Boolean = false
  var storedDeviate: Double = 1.0
  val random = new MersenneTwister(Platform.currentTime)

  override def getValue: Double =

    if (hasDeviate) {
      hasDeviate = false
      storedDeviate * standardDeviation + mean
    } else {

      storedDeviate = 100.0
      hasDeviate = true
      storedDeviate
    }


}

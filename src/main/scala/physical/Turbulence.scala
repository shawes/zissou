package physical

import org.apache.commons.math3.random.MersenneTwister

import scala.compat.Platform

class Turbulence(val horizontalDiffusionCoefficient: Double, val verticalDiffusionCoefficient: Double) {
  var random = new MersenneTwister(Platform.currentTime)

  def this() = this(0, 0)

  def apply(v: Velocity): Velocity = {
    val horizontalTurbulence = horizontalDiffusionCoefficient * random.nextDouble()
    val verticalTurbulence = verticalDiffusionCoefficient * random.nextDouble()
    new Velocity(v.u + horizontalTurbulence, v.v + horizontalTurbulence, v.w + verticalTurbulence)
  }

}

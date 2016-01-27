package physical

import maths.RandomNumberGenerator


class Turbulence(val horizontalDiffusionCoefficient: Double, val verticalDiffusionCoefficient: Double, val randomNumbers: RandomNumberGenerator) {

  def this() = this(0, 0, new RandomNumberGenerator(0))

  def apply(v: Velocity): Velocity = {
    val horizontalTurbulence = horizontalDiffusionCoefficient * randomNumbers.get
    val verticalTurbulence = verticalDiffusionCoefficient * randomNumbers.get
    new Velocity(v.u + horizontalTurbulence, v.v + horizontalTurbulence, v.w + verticalTurbulence)
  }

}

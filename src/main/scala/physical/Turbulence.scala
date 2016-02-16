package physical

import maths.RandomNumberGenerator

class Turbulence(horizontalDiffusionCoefficient: Double, verticalDiffusionCoefficient: Double, timeStep: Int, randomNumbers: RandomNumberGenerator) {

  val horizontalTurbulence = Math.pow((2 * horizontalDiffusionCoefficient) / timeStep, 0.5)
  val verticalTurbulence = Math.pow((2 * verticalDiffusionCoefficient) / timeStep, 0.5)

  def this() = this(0, 0, 0, new RandomNumberGenerator(0))

  def apply(v: Velocity): Velocity = {
    val horizontal = horizontalTurbulence * randomNumbers.get
    val vertical = verticalTurbulence * randomNumbers.get
    new Velocity(v.u + horizontal, v.v + horizontal, v.w + vertical)
  }

}

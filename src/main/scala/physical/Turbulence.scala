package physical

import maths.RandomNumberGenerator

class Turbulence(horizontalDiffusionCoefficient: Double, verticalDiffusionCoefficient: Double, timeStep: Int) {

  val horizontalTurbulence = Math.pow((2 * horizontalDiffusionCoefficient) / timeStep, 0.5)
  val verticalTurbulence = Math.pow((2 * verticalDiffusionCoefficient) / timeStep, 0.5)

  def this() = this(0, 0, 0)

  def apply(v: Velocity): Velocity = {
    val horizontal = horizontalTurbulence * RandomNumberGenerator.get(-1,1)
    val vertical = verticalTurbulence * RandomNumberGenerator.get(-1,1)
    new Velocity(v.u + horizontal, v.v + horizontal, v.w + vertical)
  }

}

package physical

import maths.{Random, RandomNumberGenerator}

class Turbulence(horizontalDiffusionCoefficient: Double, verticalDiffusionCoefficient: Double, timeStep: Int, random: Random) {

  val horizontalTurbulence = Math.pow((2 * horizontalDiffusionCoefficient) / timeStep, 0.5)
  val verticalTurbulence = Math.pow((2 * verticalDiffusionCoefficient) / timeStep, 0.5)

  def this() = this(0, 0, 0, RandomNumberGenerator)

  def apply(v: Velocity): Velocity = {
    val horizontal = horizontalTurbulence * random.get
    val vertical = verticalTurbulence * random.get
    new Velocity(v.u + horizontal, v.v + horizontal, v.w + vertical)
  }

}

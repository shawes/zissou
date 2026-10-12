package physical

import maths.RandomNumberGenerator

/**
  * Turbulent mixing, as a random walk, from diffusion coefficients in m2/s.
  *
  * A diffusivity K spreads a patch of larvae so that, along each axis, the variance of
  * their displacements grows as 2*K*t. A walk in steps of `timeStep` seconds does that when
  * every step moves a larva by a normally distributed distance with variance 2*K*timeStep,
  * which, as a speed held for the step, has standard deviation sqrt(2*K/timeStep). The
  * east-west, north-south and vertical steps are drawn independently of each other.
  */
class Turbulence(horizontalDiffusionCoefficient: Double, verticalDiffusionCoefficient: Double, timeStep: Int) {

  // the standard deviation, in m/s, of the speed this adds for one time step
  val horizontalTurbulence = Math.pow((2 * horizontalDiffusionCoefficient) / timeStep, 0.5)
  val verticalTurbulence = Math.pow((2 * verticalDiffusionCoefficient) / timeStep, 0.5)

  def this() = this(0, 0, 0)

  def apply(v: Velocity): Velocity =
    new Velocity(
      v.u + horizontalTurbulence * RandomNumberGenerator.gaussian,
      v.v + horizontalTurbulence * RandomNumberGenerator.gaussian,
      v.w + verticalTurbulence * RandomNumberGenerator.gaussian
    )

}

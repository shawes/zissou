package maths.interpolation.cubic

import physical.Velocity

class BicubicInterpolator extends CubicInterpolator {

  val partialResult: Array[Velocity] = Array.ofDim[Velocity](4)

  def interpolate(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    partialResult(0) = interpolate(v(0), y)
    partialResult(1) = interpolate(v(1), y)
    partialResult(2) = interpolate(v(2), y)
    partialResult(3) = interpolate(v(3), y)
    interpolate(partialResult, x)
  }
}

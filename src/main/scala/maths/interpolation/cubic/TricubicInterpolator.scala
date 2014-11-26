package maths.interpolation.cubic

import physical.Velocity

class TricubicInterpolator extends BicubicInterpolator {

  override val partialResult: Array[Velocity] = new Array[Velocity](4)

  def interpolate(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    partialResult(0) = interpolate(v(0), y, z)
    partialResult(1) = interpolate(v(1), y, z)
    partialResult(2) = interpolate(v(2), y, z)
    partialResult(3) = interpolate(v(3), y, z)
    interpolate(partialResult, x)
  }
}

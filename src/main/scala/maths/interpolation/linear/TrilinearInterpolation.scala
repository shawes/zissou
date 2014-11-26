package maths.interpolation.linear

import physical.Velocity

class TrilinearInterpolation extends BilinearInterpolator {

  override val partialResult: Array[Velocity] = new Array[Velocity](2)

  def interpolate(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    partialResult(0) = interpolate(v(0), y, z)
    partialResult(1) = interpolate(v(1), y, z)
    interpolate(partialResult, x)
  }
}

package maths.interpolation.linear

import physical.Velocity

class BilinearInterpolator extends LinearInterpolator {

  val partialResult: Array[Velocity] = new Array[Velocity](2)

  def interpolate(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    partialResult(0) = interpolate(v(0), y)
    partialResult(1) = interpolate(v(1), y)
    interpolate(partialResult, x)
  }
}

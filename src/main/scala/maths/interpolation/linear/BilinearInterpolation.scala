package maths.interpolation.linear

import physical.Velocity

class BilinearInterpolation extends LinearInterpolation {

  def apply(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    val partialResult = Array(this(v(0), y), this(v(1), y))
    this(partialResult, x)
  }
}

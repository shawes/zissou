package maths.interpolation.cubic

import physical.Velocity

class BicubicInterpolation extends CubicInterpolation {

  def apply(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    val partialResult =
      Array(this(v(0), y), this(v(1), y), this(v(2), y), this(v(3), y))
    this(partialResult, x)
  }
}

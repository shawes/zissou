package maths.interpolation.cubic

import physical.Velocity

class TricubicInterpolation extends BicubicInterpolation {

  def apply(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    val partialResult =
      Array(this(v(0), y, z), this(v(1), y, z), this(v(2), y, z), this(v(3), y, z))
    this(partialResult, x)
  }
}

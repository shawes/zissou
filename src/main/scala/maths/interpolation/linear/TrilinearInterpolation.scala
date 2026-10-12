package maths.interpolation.linear

import physical.Velocity

class TrilinearInterpolation extends BilinearInterpolation {

  def apply(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    val partialResult = Array(this(v(0), y, z), this(v(1), y, z))
    this(partialResult, x)
  }
}

package maths.interpolation.linear

import physical.Velocity

class LinearInterpolation {
  def apply(v: Array[Velocity], x: Double): Velocity = v(0) + (v(1) - v(0)) * x
}

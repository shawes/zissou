package maths.interpolation.linear

import physical.Velocity

class LinearInterpolator {
  def interpolate(v: Array[Velocity], x: Double): Velocity = v(0) + (v(1) - v(0)) * x
}

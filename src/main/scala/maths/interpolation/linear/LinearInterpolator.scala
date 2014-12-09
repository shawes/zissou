package maths.interpolation.linear

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class LinearInterpolator {
  def interpolate(v: ArrayBuffer[Velocity], x: Double): Velocity = v(0) + (v(1) - v(0)) * x
}

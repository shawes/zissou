package maths.interpolation.cubic

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class CubicInterpolator {
  def interpolate(v: ArrayBuffer[Velocity], x: Double): Velocity = {
    v(1) + (v(2) - v(0) + (v(0) * 2.0 - v(1) * 5.0 + v(2) * 4.0 - v(3) + ((v(1) - v(2)) * 3.0 + v(3) - v(0)) * x) * x) * x * 0.5
  }

}

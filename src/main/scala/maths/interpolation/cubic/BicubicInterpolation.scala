package maths.interpolation.cubic

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class BicubicInterpolation extends CubicInterpolation {

  val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def interpolate(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    partialResult += interpolate(v(0), y)
    partialResult += interpolate(v(1), y)
    partialResult += interpolate(v(2), y)
    partialResult += interpolate(v(3), y)
    interpolate(partialResult.toArray, x)
  }
}

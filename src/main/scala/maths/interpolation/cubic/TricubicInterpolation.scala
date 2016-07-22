package maths.interpolation.cubic

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class TricubicInterpolation extends BicubicInterpolation {

  override val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def interpolate(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    partialResult += interpolate(v(0), y, z)
    partialResult += interpolate(v(1), y, z)
    partialResult += interpolate(v(2), y, z)
    partialResult += interpolate(v(3), y, z)
    interpolate(partialResult.toArray, x)
  }
}

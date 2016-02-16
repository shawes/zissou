package maths.interpolation.linear

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class TrilinearInterpolation extends BilinearInterpolator {

  override val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def interpolate(v: ArrayBuffer[ArrayBuffer[ArrayBuffer[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    partialResult += interpolate(v(0), y, z)
    partialResult += interpolate(v(1), y, z)
    interpolate(partialResult, x)
  }
}

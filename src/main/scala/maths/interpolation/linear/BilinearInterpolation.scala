package maths.interpolation.linear

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class BilinearInterpolation extends LinearInterpolation {

  val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def interpolate(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    partialResult += interpolate(v(0), y)
    partialResult += interpolate(v(1), y)
    interpolate(partialResult.toArray, x)
  }
}

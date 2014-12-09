package maths.interpolation.linear

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class BilinearInterpolator extends LinearInterpolator {

  val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def interpolate(v: ArrayBuffer[ArrayBuffer[Velocity]], x: Double, y: Double): Velocity = {
    partialResult += interpolate(v(0), y)
    partialResult += interpolate(v(1), y)
    interpolate(partialResult, x)
  }
}

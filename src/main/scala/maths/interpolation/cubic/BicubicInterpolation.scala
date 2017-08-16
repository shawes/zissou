package maths.interpolation.cubic

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class BicubicInterpolation extends CubicInterpolation {

  val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def apply(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
    this.synchronized {
      partialResult += this (v(0), y)
      partialResult += this (v(1), y)
      partialResult += this (v(2), y)
      partialResult += this (v(3), y)
      this (partialResult.toArray, x)
    }
  }
}

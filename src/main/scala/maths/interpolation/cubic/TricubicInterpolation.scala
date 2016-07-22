package maths.interpolation.cubic

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class TricubicInterpolation extends BicubicInterpolation {

  override val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def apply(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    partialResult += this (v(0), y, z)
    partialResult += this (v(1), y, z)
    partialResult += this (v(2), y, z)
    partialResult += this (v(3), y, z)
    this (partialResult.toArray, x)
  }
}

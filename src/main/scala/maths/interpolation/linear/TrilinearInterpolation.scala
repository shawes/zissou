package maths.interpolation.linear

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class TrilinearInterpolation extends BilinearInterpolation {

  override val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def apply(v: Array[Array[Array[Velocity]]], x: Double, y: Double, z: Double): Velocity = {
    partialResult += this (v(0), y, z)
    partialResult += this (v(1), y, z)
    this (partialResult.toArray, x)
  }
}

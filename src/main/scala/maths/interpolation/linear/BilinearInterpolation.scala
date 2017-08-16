package maths.interpolation.linear

import physical.Velocity

import scala.collection.mutable.ArrayBuffer

class BilinearInterpolation extends LinearInterpolation {

  val partialResult: ArrayBuffer[Velocity] = ArrayBuffer.empty[Velocity]

  def apply(v: Array[Array[Velocity]], x: Double, y: Double): Velocity = {
  this.synchronized {
    partialResult += this (v(0), y)
    partialResult += this (v(1), y)
    this (partialResult.toArray, x)
    }
  }
}

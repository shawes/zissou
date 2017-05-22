package maths

import org.apache.commons.math3.random.MersenneTwister

import scala.compat.Platform

trait Random {
  def get: Double

  def get(low: Double, high: Double): Double

  def getPlusMinus: Double

  def coinToss: Boolean

  def seed: Long
}

object RandomNumberGenerator extends Random {

  private val random = new MersenneTwister(seed)

  def seed: Long = 82723651

  def get: Double = random.nextDouble

  def get(low: Double, high: Double): Double = random.nextDouble * (high - low) + low

  def getPlusMinus: Double = {
    val number = random.nextDouble()
    if (random.nextBoolean) {
      number
    } else {
      number * (-1.0)
    }
  }

  def coinToss: Boolean = random.nextBoolean()
}

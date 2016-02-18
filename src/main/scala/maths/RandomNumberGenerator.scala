package maths

import org.apache.commons.math3.random.MersenneTwister


class RandomNumberGenerator(seed: Long) {
  private val random = new MersenneTwister(seed)

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



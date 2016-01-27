package maths

import org.apache.commons.math3.random.MersenneTwister


class RandomNumberGenerator(seed: Long) {
  private val random = new MersenneTwister(seed)

  //TODO: Think about how to change the seed
  def get: Double = random.nextDouble
}



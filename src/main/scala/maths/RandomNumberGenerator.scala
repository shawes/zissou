package maths

import org.apache.commons.math3.random.{SynchronizedRandomGenerator,Well44497b}
import scala.compat.Platform
import com.github.nscala_time.time.Imports._

trait Random {
  def get: Double

  def get(low: Double, high: Double): Double

  def getPlusMinus: Double

  def getPercent : Double

  def getAngle : Double

  def coinToss: Boolean

  def setSeed(seed : Int)
}

object RandomNumberGenerator extends Random {

  private var random = new SynchronizedRandomGenerator(new Well44497b(DateTime.now.getMillisOfSecond))

  def setSeed(customSeed : Int) {
    random = new SynchronizedRandomGenerator(new Well44497b(customSeed))
  }

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

  def getPercent : Double = get(0,100)

  def getAngle : Double = get(0,360)
}

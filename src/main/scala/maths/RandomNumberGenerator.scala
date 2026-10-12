package maths

import java.util.SplittableRandom
import org.apache.commons.math3.random.{
  AbstractRandomGenerator,
  RandomGenerator,
  SynchronizedRandomGenerator,
  Well44497b
}
import com.github.nscala_time.time.Imports._

trait Random {
  def get: Double

  def get(low: Double, high: Double): Double

  def getPlusMinus: Double

  def getPercent: Double

  def getAngle: Double

  def coinToss: Boolean

  def setSeed(seed: Int): Unit
}

/**
  * The model's source of randomness.
  *
  * Outside of [[withStream]] every call draws from one seeded sequential
  * generator. Inside it, calls draw from a stream that is derived only from
  * the seed, a key and a step, so work that runs in parallel (one larva per
  * task) gets the same numbers however the threads are scheduled.
  */
object RandomNumberGenerator extends Random {

  private var masterSeed: Long = DateTime.now.getMillisOfSecond
  private var random: RandomGenerator = newGenerator(masterSeed)
  private val stream = new ThreadLocal[SplittableRandom]

  private def newGenerator(seed: Long): RandomGenerator =
    new SynchronizedRandomGenerator(new Well44497b(seed))

  def setSeed(customSeed: Int): Unit = {
    masterSeed = customSeed.toLong
    random = newGenerator(masterSeed)
  }

  def seed: Long = masterSeed

  /**
    * Runs `body` with its random numbers drawn from a stream fixed by the
    * seed, `key` and `step`. A fresh stream per key and step keeps the result
    * independent of thread scheduling and of how much earlier steps drew.
    */
  def withStream[T](key: Long, step: Long)(body: => T): T = {
    val previous = stream.get
    stream.set(new SplittableRandom(mix(mix(mix(masterSeed) + key) + step)))
    try body
    finally {
      if (previous == null) stream.remove() else stream.set(previous)
    }
  }

  /** A stable 64-bit key for a string such as a larva id (FNV-1a). */
  def keyFor(text: String): Long =
    text.foldLeft(0xcbf29ce484222325L)((hash, c) => (hash ^ c) * 0x100000001b3L)

  /** A commons-math generator that follows the current stream, for distributions. */
  val generator: RandomGenerator = new AbstractRandomGenerator {
    def setSeed(seed: Long): Unit = ()
    def nextDouble(): Double = nextUniform()
  }

  def get: Double = nextUniform()

  def get(low: Double, high: Double): Double = nextUniform() * (high - low) + low

  def getPlusMinus: Double = {
    val number = nextUniform()
    if (coinToss) number else number * (-1.0)
  }

  def coinToss: Boolean = {
    val current = stream.get
    if (current != null) current.nextBoolean() else random.nextBoolean()
  }

  def getPercent: Double = get(0, 100)

  def getAngle: Double = get(0, 360)

  private def nextUniform(): Double = {
    val current = stream.get
    if (current != null) current.nextDouble() else random.nextDouble()
  }

  // SplitMix64 finalizer: spreads nearby inputs across the whole 64-bit range
  private def mix(value: Long): Long = {
    val a = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L
    val b = (a ^ (a >>> 27)) * 0x94d049bb133111ebL
    b ^ (b >>> 31)
  }
}

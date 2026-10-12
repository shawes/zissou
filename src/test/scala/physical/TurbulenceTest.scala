package physical

import maths.RandomNumberGenerator
import org.scalatest.matchers.should.Matchers._
import org.scalatest.flatspec.AnyFlatSpec

class TurbulenceTest extends AnyFlatSpec {

  "The turbulence object" should "initialise" in {
    val turbulence = new Turbulence()
    turbulence should not be null
  }

  it should "initialise the correct horizontal turbulence" in {
    val turbulence = new Turbulence(300, 15, 3600)
    val h_turb_cal: Double = Math.pow((2.0 * 300) / 3600, 0.5)
    turbulence.horizontalTurbulence should equal(h_turb_cal)
  }

  it should "initialise the correct vertical turbulence" in {
    val turbulence = new Turbulence(1.0, 2.0, 3)
    val v_turb_cal = Math.pow((2.0 * 2.0) / 3, 0.5)
    turbulence.verticalTurbulence should equal(v_turb_cal)
  }

  // ---- what the random walk has to do ------------------------------------------

  private val horizontalK = 300.0 // m2/s
  private val verticalK = 15.0
  private val twoHours = 7200

  /** The distance, in metres, that turbulence alone moves a larva in one step: east, north and up. */
  private def steps(turbulence: Turbulence, timeStep: Int, count: Int, seed: Int = 1): IndexedSeq[(Double, Double, Double)] = {
    RandomNumberGenerator.setSeed(seed)
    IndexedSeq.fill(count) {
      val speed = turbulence(new Velocity(0, 0, 0))
      (speed.u * timeStep, speed.v * timeStep, speed.w * timeStep)
    }
  }

  private def mean(xs: Seq[Double]) = xs.sum / xs.size
  private def variance(xs: Seq[Double]) = { val m = mean(xs); xs.map(x => (x - m) * (x - m)).sum / xs.size }
  private def correlation(a: Seq[Double], b: Seq[Double]) = {
    val (ma, mb) = (mean(a), mean(b))
    a.zip(b).map { case (x, y) => (x - ma) * (y - mb) }.sum / a.size / math.sqrt(variance(a) * variance(b))
  }
  private def kurtosis(xs: Seq[Double]) = { val m = mean(xs); xs.map(x => math.pow(x - m, 4)).sum / xs.size / math.pow(variance(xs), 2) }

  private val draws = 200000
  private lazy val walk = steps(new Turbulence(horizontalK, verticalK, twoHours), twoHours, draws)
  private lazy val east = walk.map(_._1)
  private lazy val north = walk.map(_._2)
  private lazy val up = walk.map(_._3)

  "A step of turbulence" should "have the variance of diffusion: 2 * K * time, along each axis" in {
    assert(math.abs(variance(east) / (2 * horizontalK * twoHours) - 1) < 0.02, s"east-west ${variance(east) / (2 * horizontalK * twoHours)} of the target")
    assert(math.abs(variance(north) / (2 * horizontalK * twoHours) - 1) < 0.02, s"north-south ${variance(north) / (2 * horizontalK * twoHours)} of the target")
    assert(math.abs(variance(up) / (2 * verticalK * twoHours) - 1) < 0.02, s"vertical ${variance(up) / (2 * verticalK * twoHours)} of the target")
  }

  it should "have no average push in any direction" in {
    val spread = math.sqrt(2 * horizontalK * twoHours)
    assert(math.abs(mean(east)) < 0.01 * spread && math.abs(mean(north)) < 0.01 * spread)
    assert(math.abs(mean(up)) < 0.01 * math.sqrt(2 * verticalK * twoHours))
  }

  it should "push east-west, north-south and up-down independently of each other" in {
    // the old walk added one number to both east-west and north-south: a correlation of 1
    assert(math.abs(correlation(east, north)) < 0.01, s"east and north ${correlation(east, north)}")
    assert(math.abs(correlation(east, up)) < 0.01 && math.abs(correlation(north, up)) < 0.01)
  }

  it should "be normally distributed" in {
    val spread = math.sqrt(variance(east))
    assert(math.abs(kurtosis(east) - 3.0) < 0.1, s"kurtosis ${kurtosis(east)}, a normal has 3 and a uniform has 1.8")
    assert(math.abs(east.count(x => math.abs(x) < spread).toDouble / draws - 0.6827) < 0.005)
    assert(math.abs(east.count(x => math.abs(x) < 2 * spread).toDouble / draws - 0.9545) < 0.005)
  }

  it should "have no preferred horizontal direction" in {
    val octants = Array.fill(8)(0)
    east.zip(north).foreach { case (e, n) => octants(((math.atan2(n, e) + math.Pi) / (math.Pi / 4)).toInt.min(7)) += 1 }
    octants.zipWithIndex.foreach { case (count, i) => assert(math.abs(count.toDouble / draws - 0.125) < 0.01, s"octant $i holds ${count.toDouble / draws}") }
  }

  it should "spread larvae the same however the time is divided into steps" in {
    // 24 hours of mixing, in 24 steps of an hour or in 8 steps of three: the spread of
    // positions is 2 * K * t either way
    val total = 24 * 3600
    for (timeStep <- Seq(3600, 10800)) {
      val turbulence = new Turbulence(horizontalK, verticalK, timeStep)
      RandomNumberGenerator.setSeed(timeStep)
      val finals = IndexedSeq.fill(20000) {
        var (e, n, z) = (0.0, 0.0, 0.0)
        for (_ <- 0 until total / timeStep) {
          val speed = turbulence(new Velocity(0, 0, 0))
          e += speed.u * timeStep; n += speed.v * timeStep; z += speed.w * timeStep
        }
        (e, n, z)
      }
      assert(math.abs(variance(finals.map(_._1)) / (2 * horizontalK * total) - 1) < 0.05, s"east-west with ${timeStep}s steps")
      assert(math.abs(variance(finals.map(_._2)) / (2 * horizontalK * total) - 1) < 0.05, s"north-south with ${timeStep}s steps")
      assert(math.abs(variance(finals.map(_._3)) / (2 * verticalK * total) - 1) < 0.05, s"vertical with ${timeStep}s steps")
    }
  }

  it should "add to the velocity it is given and not depend on it" in {
    val turbulence = new Turbulence(horizontalK, verticalK, twoHours)
    RandomNumberGenerator.setSeed(3)
    val alone = turbulence(new Velocity(0, 0, 0))
    RandomNumberGenerator.setSeed(3)
    val withFlow = turbulence(new Velocity(5.0, 7.0, 9.0))
    assert(math.abs(withFlow.u - 5.0 - alone.u) < 1e-12 && math.abs(withFlow.v - 7.0 - alone.v) < 1e-12 && math.abs(withFlow.w - 9.0 - alone.w) < 1e-12)
  }

  it should "leave the velocity alone when there is no diffusion" in {
    val result = new Turbulence(0.0, 0.0, twoHours)(new Velocity(5.0, 7.0, 9.0))
    assert(result.u == 5.0 && result.v == 7.0 && result.w == 9.0)
  }

  it should "be repeatable for a larva at a time, and different for another larva" in {
    val turbulence = new Turbulence(horizontalK, verticalK, twoHours)
    def kick(key: Long, step: Long) = RandomNumberGenerator.withStream(key, step) { val v = turbulence(new Velocity(0, 0, 0)); (v.u, v.v, v.w) }
    assert(kick(1, 1) == kick(1, 1))
    assert(kick(1, 1) != kick(2, 1) && kick(1, 1) != kick(1, 2))
    val (u, v, _) = kick(1, 1)
    assert(u != v, "east-west and north-south are separate draws")
  }
}

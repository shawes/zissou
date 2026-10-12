package biology.swimming

import locals._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class HorizontalSwimmingTest
    extends AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  def fixture =
    new {
      val swimmingStrategy1 =
        new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)
      val swimmingStrategy2 =
        new HorizontalSwimming(Directed, StrategyTwo, 1, 1, 1, true, 1)
      val swimmingStrategy3 =
        new HorizontalSwimming(Directed, StrategyThree, 1, 1, 1, true, 1)
    }

  "A horizontal swimming object" should "initialise" in {
    val swimmingStrategy1 =
      new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)
    assert(swimmingStrategy1 != null)
  }

  it should "choose strategy 1 if initialised" in {
    val swimmingStrategy1 =
      new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)
    val speed =
      swimmingStrategy1(new HorizontalSwimmingVariables(1, 1, 1, 1, 1, 1))
    assert(!speed.isDefined)
  }

  // A larva past postflexion swimming with strategy two goes at exactly the critical speed,
  // so the velocity it ends up with depends only on the heading.
  private val criticalSpeed = 0.2

  private def swim(swimming: HorizontalSwimming, angle: Double) =
    swimming(new HorizontalSwimmingVariables(angle, 10, 2, 5, 8, 20)).get

  private def directed = new HorizontalSwimming(Directed, StrategyTwo, criticalSpeed, 1, 1, true, 1)

  "The heading of a directed larva" should "be in degrees, counter-clockwise from east" in {
    val half = math.sqrt(0.5)
    for ((degrees, east, north) <- Seq((0.0, 1.0, 0.0), (90.0, 0.0, 1.0), (180.0, -1.0, 0.0), (270.0, 0.0, -1.0),
      (45.0, half, half), (135.0, -half, half), (-90.0, 0.0, -1.0), (360.0, 1.0, 0.0), (-135.0, -half, -half))) {
      val velocity = swim(directed, degrees)
      assert(math.abs(velocity.u - criticalSpeed * east) < 1e-9, s"$degrees degrees: u was ${velocity.u}")
      assert(math.abs(velocity.v - criticalSpeed * north) < 1e-9, s"$degrees degrees: v was ${velocity.v}")
      assert(velocity.w == 0.0)
    }
  }

  it should "give the same speed whichever way it is going" in {
    for (degrees <- -360 to 360 by 7) {
      val velocity = swim(directed, degrees.toDouble)
      assert(math.abs(math.hypot(velocity.u, velocity.v) - criticalSpeed) < 1e-9, s"$degrees degrees")
    }
  }

  private def headings(swimming: HorizontalSwimming, angle: Double, draws: Int) = {
    maths.RandomNumberGenerator.setSeed(7)
    (0 until draws).map(_ => swim(swimming, angle))
  }

  private def coversAllDirectionsEvenly(velocities: Seq[physical.Velocity]): Unit = {
    val share = (inQuadrant: physical.Velocity => Boolean) => velocities.count(inQuadrant).toDouble / velocities.size
    assert(share(v => v.u > 0 && v.v > 0) > 0.2 && share(v => v.u > 0 && v.v > 0) < 0.3)
    assert(share(v => v.u < 0 && v.v > 0) > 0.2 && share(v => v.u < 0 && v.v > 0) < 0.3)
    assert(share(v => v.u < 0 && v.v < 0) > 0.2 && share(v => v.u < 0 && v.v < 0) < 0.3)
    assert(share(v => v.u > 0 && v.v < 0) > 0.2 && share(v => v.u > 0 && v.v < 0) < 0.3)
    assert(velocities.forall(v => math.abs(math.hypot(v.u, v.v) - criticalSpeed) < 1e-9))
  }

  "A larva that cannot be directed" should "head in every direction equally, whatever heading it is given" in {
    coversAllDirectionsEvenly(headings(new HorizontalSwimming(Undirected, StrategyTwo, criticalSpeed, 1, 1, true, 1), 0.0, 4000))
  }

  "A directed larva with no direction yet" should "head in every direction equally" in {
    coversAllDirectionsEvenly(headings(directed, locals.Constants.LightWeightException.NoSwimmingAngleException.toDouble, 4000))
  }

  "A larva that is not yet swimming" should "have no velocity, whatever its heading" in {
    val tooYoung = new HorizontalSwimmingVariables(90.0, 1, 1, 1, 1, 1)
    assert(new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)(tooYoung).isEmpty)
  }
}

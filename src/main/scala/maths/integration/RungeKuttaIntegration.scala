package maths.integration

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import maths.Geometry
import physical.flow.FlowController
import physical.{GeoCoordinate, Turbulence, Velocity}

/**
  * Carries a larva forward by a time step through the flow, by the classical fourth order
  * Runge-Kutta method.
  *
  * The flow is sampled four times: at the start of the step (k1); in the middle of it, from
  * where k1 would take the larva by then (k2); in the middle again, from where k2 would (k3); and
  * at the end, from where k3 would (k4). The larva moves at the weighted mean of the four,
  * (k1 + 2 k2 + 2 k3 + k4) / 6. Each is the flow at its own time, so a flow that changes with
  * time is followed through the step.
  *
  * What carries the larva to those sample points is the flow and its own swimming. Turbulence is
  * a random speed added to the mean flow for the whole step.
  */
class RungeKuttaIntegration(flow: FlowController, turbulence: Option[Turbulence], timeStep: Int) extends Logging {

  private val geometry = new Geometry
  private val notSwimming = new Velocity(0, 0, 0)

  /**
    * Where the larva is a time step on, or None where it cannot go at all: it begins on land, or the flow carries it
    * into the sea floor or a corner of the coast. A step that would end on land is shortened, or made along the coast.
    */
  def integrate(coordinate: GeoCoordinate, time: LocalDateTime, swimming: Option[Velocity]): Option[GeoCoordinate] = {
    val swim = swimming.getOrElse(notSwimming)
    val includeVerticalVelocity = flow.flow.includeVerticalVelocity
    val middle = time.plusMillis(timeStep * 500)
    val end = time.plusSeconds(timeStep)

    def flowAt(position: GeoCoordinate, at: LocalDateTime): Option[Velocity] =
      flow.getVelocityOfCoordinate(position, at)

    // where the larva is, some seconds on from the start, moving at a velocity and swimming
    def after(seconds: Double, velocity: Velocity): GeoCoordinate =
      geometry.translatePoint(coordinate, velocity, seconds, swim, includeVerticalVelocity)

    // The flow where the larva starts: with none there, on land, there is nothing to move it. At the other samples,
    // beyond what the first would carry it, a coast in the data is a cell or two out from the real one: where the larva is
    // sampled over land it is given the flow of the sample before, so that one sample off the coast does not stop it.
    flowAt(coordinate, time).flatMap { k1 =>
      val k2 = flowAt(after(timeStep / 2.0, k1), middle).getOrElse(k1)
      val k3 = flowAt(after(timeStep / 2.0, k2), middle).getOrElse(k2)
      val k4 = flowAt(after(timeStep, k3), end).getOrElse(k3)
      val mean = (k1 + k2 * 2.0 + k3 * 2.0 + k4) * (1.0 / 6.0)
      val velocity = turbulence.map(_.apply(mean)).getOrElse(mean)
      val point = after(timeStep, velocity)
      if (isSea(point)) Some(point)
      else tryMovingLarvaeUp(point, 10.0).orElse(alongTheCoast(coordinate, point))
    }
  }

  private def isSea(position: GeoCoordinate): Boolean = flow.getVelocity(position).isDefined

  /**
    * Where a step that ends on land can go instead: the farthest it can get towards where it was going that is in the
    * sea. The step is tried a part of the way (three quarters, a half, a quarter), and along each of the two axes on its
    * own, so that a larva carried into a coast and along it goes on along the coast, and one carried straight at it
    * goes as far as the coast. None where it can go nowhere, as when it is carried straight into a corner.
    */
  private def alongTheCoast(from: GeoCoordinate, to: GeoCoordinate): Option[GeoCoordinate] = {
    val east = to.longitude - from.longitude
    val north = to.latitude - from.latitude
    val cosine = math.cos(math.toRadians(from.latitude))
    val candidates = for {
      part <- Seq(1.0, 0.75, 0.5, 0.25)
      (de, dn) <- Seq((east, north), (east, 0.0), (0.0, north))
      if !(part == 1.0 && de != 0.0 && dn != 0.0) // the whole step, in both, is what failed
      if de != 0.0 || dn != 0.0
    } yield new GeoCoordinate(from.latitude + dn * part, from.longitude + de * part, to.depth)
    candidates
      .filter(isSea)
      .maxByOption(p => math.pow((p.longitude - from.longitude) * cosine, 2) + math.pow(p.latitude - from.latitude, 2))
  }

  private def tryMovingLarvaeUp(position: GeoCoordinate, amount: Double): Option[GeoCoordinate] = {
    if (position.depth > amount) {
      val higherPoint = new GeoCoordinate(position.latitude, position.longitude, position.depth - amount)
      if (flow.getVelocity(higherPoint).isDefined) Some(higherPoint)
      else None
    } else None
  }
}

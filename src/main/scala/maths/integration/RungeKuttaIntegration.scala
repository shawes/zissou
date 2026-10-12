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

  /** Where the larva is a time step on, or None where the flow does not take it (the sea floor or the coast). */
  def integrate(coordinate: GeoCoordinate, time: LocalDateTime, swimming: Option[Velocity], dampening: Double): Option[GeoCoordinate] = {
    val swim = swimming.getOrElse(notSwimming)
    val includeVerticalVelocity = flow.flow.includeVerticalVelocity
    val middle = time.plusMillis(timeStep * 500)
    val end = time.plusSeconds(timeStep)

    def flowAt(position: GeoCoordinate, at: LocalDateTime): Option[Velocity] =
      flow.getVelocityOfCoordinate(position, at).map(_ * dampening)

    // where the larva is, some seconds on from the start, moving at a velocity and swimming
    def after(seconds: Double, velocity: Velocity): GeoCoordinate =
      geometry.translatePoint(coordinate, velocity, seconds, swim, includeVerticalVelocity)

    val meanFlow = for {
      k1 <- flowAt(coordinate, time)
      k2 <- flowAt(after(timeStep / 2.0, k1), middle)
      k3 <- flowAt(after(timeStep / 2.0, k2), middle)
      k4 <- flowAt(after(timeStep, k3), end)
    } yield (k1 + k2 * 2.0 + k3 * 2.0 + k4) * (1.0 / 6.0)

    meanFlow.flatMap { mean =>
      val velocity = turbulence.map(_.apply(mean)).getOrElse(mean)
      val point = after(timeStep, velocity)
      if (flow.getVelocity(point).isDefined) Some(point)
      else tryMovingLarvaeUp(point, 10.0)
    }
  }

  private def tryMovingLarvaeUp(position: GeoCoordinate, amount: Double): Option[GeoCoordinate] = {
    if (position.depth > amount) {
      val higherPoint = new GeoCoordinate(position.latitude, position.longitude, position.depth - amount)
      if (flow.getVelocity(higherPoint).isDefined) Some(higherPoint)
      else None
    } else None
  }
}

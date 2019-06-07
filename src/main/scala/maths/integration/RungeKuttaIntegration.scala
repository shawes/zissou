package maths.integration

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import locals._
import maths.Geometry
import physical.flow.FlowController
import physical.{GeoCoordinate, Turbulence, Velocity}

class RungeKuttaIntegration(
    flow: FlowController,
    turbulence: Option[Turbulence],
    timeStep: Int,
    backtracking: Boolean
) extends Logging {

  val geometry = new Geometry

  def integrate(
      coordinate: GeoCoordinate,
      time: LocalDateTime,
      swimming: Option[Velocity],
      dampening: Double
  ): Option[GeoCoordinate] = {
    flow.getVelocity(coordinate) match {
      case Some(velocity) =>
        performRungeKuttaSteps(velocity, coordinate, time, swimming, dampening)
      case None => None
    }
  }

  private def performRungeKuttaSteps(
      velocity: Velocity,
      coordinate: GeoCoordinate,
      time: LocalDateTime,
      swimming: Option[Velocity],
      dampening: Double
  ): Option[GeoCoordinate] = {
    val step1 = performRungeKuttaIteration(
      coordinate,
      Some(velocity * dampening),
      timeStep,
      time,
      swimming
    )
    if (step1.velocity.isDefined) {
      val step2 = performRungeKuttaIteration(
        coordinate,
        Some(step1.velocity.get * dampening),
        (timeStep * 1.5).toInt,
        time,
        swimming
      )
      if (step2.velocity.isDefined) {
        val step3 = performRungeKuttaIteration(
          coordinate,
          Some(step2.velocity.get * dampening),
          (timeStep * 1.5).toInt,
          time,
          swimming
        )
        if (step3.velocity.isDefined) {
          val step4 = performRungeKuttaIteration(
            coordinate,
            Some(step3.velocity.get * dampening),
            timeStep * 2,
            time,
            swimming
          )
          if (step4.velocity.isDefined) {
            val u = (step1.velocity.get.u + (2 * step2.velocity.get.u) + (2 * step3.velocity.get.u) + step4.velocity.get.u) * 0.16666
            val v = (step1.velocity.get.v + (2 * step2.velocity.get.v) + (2 * step3.velocity.get.v) + step4.velocity.get.v) * 0.16666
            val w = (step1.velocity.get.w + (2 * step2.velocity.get.w) + (2 * step3.velocity.get.w) + step4.velocity.get.w) * 0.16666

            var integratedVelocity = new Velocity(u, v, w)

            if (turbulence.isDefined)
              integratedVelocity = turbulence.get.apply(integratedVelocity)

            val point = geometry.translatePoint(
              coordinate,
              integratedVelocity,
              timeStep,
              swimming.getOrElse(new Velocity(0, 0, 0)),
              flow.flow.includeVerticalVelocity,
              backtracking
            )

            if (flow.getVelocity(point).isDefined) Some(point)
            else tryMovingLarvaeUp(point, 10.0)

          } else {
            None
          }
        } else {
          None
        }
      } else {
        None
      }
    } else {
      None
    }
  }

  private def tryMovingLarvaeUp(
      position: GeoCoordinate,
      amount: Double
  ): Option[GeoCoordinate] = {
    if (position.depth > amount) {
      val higherPoint = new GeoCoordinate(
        position.latitude,
        position.longitude,
        position.depth - amount
      )
      if (flow.getVelocity(higherPoint).isDefined) Some(higherPoint)
      else None
    } else None
  }

  private def performRungeKuttaIteration(
      coordinate: GeoCoordinate,
      velocity: Option[Velocity],
      partialTimeStep: Int,
      time: LocalDateTime,
      swimming: Option[Velocity]
  ): RungeKuttaStepDerivative = {
    if (velocity.isDefined) {
      val normalisedTime = partialTimeStep - timeStep
      val newCoordinate = geometry.translatePoint(
        coordinate,
        velocity.get,
        partialTimeStep,
        swimming.getOrElse(new Velocity(0, 0, 0)),
        flow.flow.includeVerticalVelocity,
        backtracking
      )
      val newVelocity = flow.getVelocityOfCoordinate(
        newCoordinate,
        time.plusSeconds(normalisedTime),
        time,
        partialTimeStep
      )
      new RungeKuttaStepDerivative(newVelocity, newCoordinate)
    } else {
      new RungeKuttaStepDerivative(None, coordinate)
    }
  }
}

case class RungeKuttaStepDerivative(
    val velocity: Option[Velocity],
    val coordinate: GeoCoordinate
)

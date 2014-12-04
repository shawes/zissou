package maths.integration

import com.github.nscala_time.time.Imports._
import maths.Geometry
import physical.flow.FlowController
import physical.{GeoCoordinate, Velocity}

class RungeKuttaIntegration(FlowController: FlowController, timeStep: Int) {

  val geometry = new Geometry

  def integrate(coordinate: GeoCoordinate, time: DateTime, speed: Double): GeoCoordinate = {

    val velocity: Velocity = FlowController.getVelocityOfCoordinate(coordinate, isFuture = false)
    val step1 = performRungeKuttaIteration(coordinate, velocity, timeStep, time)
    if (step1.noValueFound) return new GeoCoordinate(Double.NaN, Double.NaN) //TODO Turn into exception

    val step2 = performRungeKuttaIteration(coordinate, step1.velocity, (timeStep * 1.5).toInt, time)
    if (step2.noValueFound) return new GeoCoordinate(Double.NaN, Double.NaN)

    val step3 = performRungeKuttaIteration(coordinate, step2.velocity, (timeStep * 1.5).toInt, time)
    if (step3.noValueFound) return new GeoCoordinate(Double.NaN, Double.NaN)

    val step4 = performRungeKuttaIteration(coordinate, step3.velocity, timeStep * 2, time)
    if (step4.noValueFound) return new GeoCoordinate(Double.NaN, Double.NaN)

    val u = (step1.velocity.u + (2 * step2.velocity.u) + (2 * step3.velocity.u) + step4.velocity.u) * 0.16666
    val v = (step1.velocity.v + (2 * step2.velocity.v) + (2 * step3.velocity.v) + step4.velocity.v) * 0.16666
    val w = (step1.velocity.w + (2 * step2.velocity.w) + (2 * step3.velocity.w) + step4.velocity.w) * 0.16666

    val integratedVelocity = new Velocity(u, v, w)
    geometry.translatePoint(coordinate, integratedVelocity, timeStep, speed)

  }

  private def performRungeKuttaIteration(coordinate: GeoCoordinate, velocity: Velocity,
                                         partialTimeStep: Int, time: DateTime): RungeKuttaStepDerivative = {
    if (coordinate.isUndefined || velocity.isUndefined) return new RungeKuttaStepDerivative(null, null, true)

    val normalisedTime: Int = partialTimeStep - timeStep

    val newCoordinate = geometry.translatePoint(coordinate, velocity, partialTimeStep)
    val newVelocity = FlowController.getVelocityOfCoordinate(newCoordinate, time.plusSeconds(normalisedTime), time, partialTimeStep)
    new RungeKuttaStepDerivative(newVelocity, newCoordinate, velocity.isUndefined)

  }
}

case class RungeKuttaStepDerivative(velocity: Velocity, coordinate: GeoCoordinate, noValueFound: Boolean)

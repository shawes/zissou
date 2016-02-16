package maths.integration

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import maths.Geometry
import physical.flow.FlowController
import physical.{GeoCoordinate, Turbulence, Velocity}

class RungeKuttaIntegration(flow: FlowController, turbulence: Turbulence, timeStep: Int) extends Logging {

  val geometry = new Geometry

  def integrate(coordinate: GeoCoordinate, time: DateTime, speed: Double): GeoCoordinate = {

    val coordinateVelocity = flow.getVelocityOfCoordinate(coordinate, isFuture = false)
    debug("Starting an RK4 integration on coord " + coordinate + "with velocity " + coordinateVelocity)
    val velocity = turbulence.apply(coordinateVelocity)

    //debug("Got the velocity " + velocity + " at the location " + coordinate)
    //debug("Performing step 1")
    val step1 = performRungeKuttaIteration(coordinate, velocity, timeStep, time)
    if (step1.velocity.isUndefined) return step1.coordinate //TODO Turn into exception
    //debug("Performing step 2")
    val step2 = performRungeKuttaIteration(coordinate, step1.velocity, (timeStep * 1.5).toInt, time)
    if (step2.velocity.isUndefined) return step2.coordinate
    //debug("Performing step 3")
    val step3 = performRungeKuttaIteration(coordinate, step2.velocity, (timeStep * 1.5).toInt, time)
    if (step3.velocity.isUndefined) return step3.coordinate
    //debug("Performing step 4")
    val step4 = performRungeKuttaIteration(coordinate, step3.velocity, timeStep * 2, time)
    if (step4.velocity.isUndefined) return step4.coordinate

    val u = (step1.velocity.u + (2 * step2.velocity.u) + (2 * step3.velocity.u) + step4.velocity.u) * 0.16666
    val v = (step1.velocity.v + (2 * step2.velocity.v) + (2 * step3.velocity.v) + step4.velocity.v) * 0.16666
    val w = (step1.velocity.w + (2 * step2.velocity.w) + (2 * step3.velocity.w) + step4.velocity.w) * 0.16666

    val integratedVelocity = new Velocity(u, v, w)
    //debug("Integrated velocity is " + integratedVelocity)
    geometry.translatePoint(coordinate, integratedVelocity, timeStep, speed)
    //val newVelocity = flow.getVelocityOfCoordinate(point, isFuture = true)
    // If there is no velocity at the next time step, assume its land and don't move
    //if(newVelocity.isUndefined) new GeoCoordinate(Double.NaN,Double.NaN) else point
  }

  private def performRungeKuttaIteration(coordinate: GeoCoordinate, velocity: Velocity,
                                         partialTimeStep: Int, time: DateTime): RungeKuttaStepDerivative = {
    //debug("Starting an RK4 integration STEP")
    if (coordinate.isUndefined || velocity.isUndefined) {
      return new RungeKuttaStepDerivative(new Velocity(Double.NaN, Double.NaN), new GeoCoordinate(Double.NaN, Double.NaN, Double.NaN))
    }

    val normalisedTime = partialTimeStep - timeStep
    //debug("Normalised time is " + partialTimeStep + "-" + timeStep + "=" + normalisedTime)

    val newCoordinate = geometry.translatePoint(coordinate, velocity, partialTimeStep)
    //debug("New coord is " + newCoordinate)
    val newVelocity = flow.getVelocityOfCoordinate(newCoordinate, time.plusSeconds(normalisedTime), time, partialTimeStep)
    //debug("New velocity is " + newVelocity)
    if (newCoordinate.isUndefined || newVelocity.isUndefined) {
      new RungeKuttaStepDerivative(new Velocity(Double.NaN, Double.NaN), new GeoCoordinate(Double.NaN, Double.NaN, Double.NaN))
    } else {
      new RungeKuttaStepDerivative(newVelocity, newCoordinate)
    }

  }
}

case class RungeKuttaStepDerivative(velocity: Velocity, coordinate: GeoCoordinate)

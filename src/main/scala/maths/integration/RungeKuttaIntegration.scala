package maths.integration

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import locals.Day._
import maths.Geometry
import physical.flow.FlowController
import physical.{GeoCoordinate, Turbulence, Velocity}

class RungeKuttaIntegration(flow: FlowController, turbulence: Turbulence, timeStep: Int) extends Logging {

  val geometry = new Geometry

  def integrate(coordinate: GeoCoordinate, time: DateTime, swimming: Option[Velocity]): Option[GeoCoordinate] = {
    flow.getVelocityOfCoordinate(coordinate, Today) match {
      case Some(velocity) => performRungeKuttaSteps(velocity, coordinate, time, swimming)
      case None => None
    }
  }

  private def performRungeKuttaSteps(velocity: Velocity, coordinate: GeoCoordinate, time: DateTime, swimming: Option[Velocity]) : Option[GeoCoordinate] = {
    val step1 = performRungeKuttaIteration(coordinate, Some(velocity), timeStep, time, swimming)
    //debug("Step1 v= " + step1.velocity + " at the location " + step1.coordinate)
    if (step1.velocity.isDefined) {
      val step2 = performRungeKuttaIteration(coordinate, step1.velocity, (timeStep * 1.5).toInt, time, swimming)
      //debug("Step2 v= " + step2.velocity + " at the location " + step2.coordinate)
      if (step2.velocity.isDefined) {
        //debug("Performing step 3")
        val step3 = performRungeKuttaIteration(coordinate, step2.velocity, (timeStep * 1.5).toInt, time, swimming)
        //debug("Step3 v= " + step3.velocity + " at the location " + step3.coordinate)
        if (step3.velocity.isDefined) {
          //debug("Performing step 4")
          val step4 = performRungeKuttaIteration(coordinate, step3.velocity, timeStep * 2, time, swimming)
          //debug("Step4 v= " + step4.velocity + " at the location " + step4.coordinate)
          if (step4.velocity.isDefined) {

            val u = (step1.velocity.get.u + (2 * step2.velocity.get.u) + (2 * step3.velocity.get.u) + step4.velocity.get.u) * 0.16666
            val v = (step1.velocity.get.v + (2 * step2.velocity.get.v) + (2 * step3.velocity.get.v) + step4.velocity.get.v) * 0.16666
            val w = (step1.velocity.get.w + (2 * step2.velocity.get.w) + (2 * step3.velocity.get.w) + step4.velocity.get.w) * 0.16666

            val integratedVelocity = new Velocity(u, v, w)
            val turbulentVelocity = turbulence.apply(integratedVelocity)

            return Some(geometry.translatePoint(coordinate, turbulentVelocity, timeStep, swimming.getOrElse(new Velocity(0, 0, 0))))
            //debug("RK4 velocity is " + integratedVelocity + " and moved to " + point)
            //point
          }
        }
      }
    }
    None
  }
    //val newVelocity = flow.getVelocityOfCoordinate(point, isFuture = true)
    // If there is no velocity at the next time step, assume its land and don't move
    //if(newVelocity.isUndefined) new GeoCoordinate(Double.NaN,Double.NaN) else point


  private def performRungeKuttaIteration(coordinate: GeoCoordinate, velocity: Option[Velocity],
                                         partialTimeStep: Int, time: DateTime, swimming: Option[Velocity]): RungeKuttaStepDerivative = {
    //debug("Starting an RK4 integration STEP")
    if (velocity.isDefined) {
      val normalisedTime = partialTimeStep - timeStep
      //debug("Normalised time is " + partialTimeStep + "-" + timeStep + "=" + normalisedTime)
      val newCoordinate = geometry.translatePoint(coordinate, velocity.get, partialTimeStep, swimming.getOrElse(new Velocity(0,0,0)))
      //debug("New coord is " + newCoordinate)
      val newVelocity = flow.getVelocityOfCoordinate(newCoordinate, time.plusSeconds(normalisedTime), time, partialTimeStep)
      //debug("New velocity is " + newVelocity)
      //if (newVelocity.isEmpty) {
      //  new RungeKuttaStepDerivative(new Velocity(Double.NaN, Double.NaN), newCoordinate)
      // } else {
        new RungeKuttaStepDerivative(newVelocity, newCoordinate)
    } else {
      new RungeKuttaStepDerivative(None, coordinate)
    }

    //}
  }
}

case class RungeKuttaStepDerivative(val velocity: Option[Velocity], val coordinate: GeoCoordinate)

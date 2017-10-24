package maths.integration

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import locals.Day._
import maths.Geometry
import physical.flow.FlowController
import physical.{GeoCoordinate, Turbulence, Velocity}

class RungeKuttaIntegration(flow: FlowController, turbulence: Option[Turbulence], timeStep: Int) extends Logging {

  val geometry = new Geometry

  def integrate(coordinate: GeoCoordinate, time: LocalDateTime, swimming: Option[Velocity], dampening: Double): Option[GeoCoordinate] = {
    flow.getVelocity(coordinate) match {
      case Some(velocity) => performRungeKuttaSteps(velocity, coordinate, time, swimming, dampening)
      case None => None
    }
  }

  private def performRungeKuttaSteps(velocity: Velocity, coordinate: GeoCoordinate, time: LocalDateTime, swimming: Option[Velocity], dampening: Double) : Option[GeoCoordinate] = {
    debug("PERFORMING: Coordinate is " +coordinate + ", velocity is " + velocity + ", on"+ time.toDate)
    val step1 = performRungeKuttaIteration(coordinate, Some(velocity*dampening), timeStep, time, swimming)
    debug("Step1 coordinate is " + step1.coordinate + ", new velocity is " + step1.velocity)
    if (step1.velocity.isDefined) {
      val step2 = performRungeKuttaIteration(coordinate, Some(step1.velocity.get*dampening), (timeStep * 1.5).toInt, time, swimming)
      debug("Step2 coordinate is " + step2.coordinate + ", new velocity is " + step2.velocity)
      if (step2.velocity.isDefined) {
        val step3 = performRungeKuttaIteration(coordinate, Some(step2.velocity.get*dampening), (timeStep * 1.5).toInt, time, swimming)
        debug("Step3 coordinate is " + step3.coordinate + ", new velocity is " + step3.velocity)
        if (step3.velocity.isDefined) {
          val step4 = performRungeKuttaIteration(coordinate, Some(step3.velocity.get*dampening), timeStep * 2, time, swimming)
          debug("Step4 coordinate is " + step4.coordinate + ", new velocity is " + step4.velocity)
          if (step4.velocity.isDefined) {
            val u = (step1.velocity.get.u + (2 * step2.velocity.get.u) + (2 * step3.velocity.get.u) + step4.velocity.get.u) * 0.16666
            val v = (step1.velocity.get.v + (2 * step2.velocity.get.v) + (2 * step3.velocity.get.v) + step4.velocity.get.v) * 0.16666
            val w = (step1.velocity.get.w + (2 * step2.velocity.get.w) + (2 * step3.velocity.get.w) + step4.velocity.get.w) * 0.16666

            var integratedVelocity = new Velocity(u, v, w)
            debug("velocity is "+ integratedVelocity)

            if(turbulence.isDefined) integratedVelocity = turbulence.get.apply(integratedVelocity)
            debug("velocity with turb is "+ integratedVelocity)

            val point = geometry.translatePoint(coordinate, integratedVelocity, timeStep, swimming.getOrElse(new Velocity(0, 0, 0)))
            debug("point is "+ point)

            val velocityAtPoint = flow.getVelocity(coordinate)
            debug("RK4: velocity at point ="+ velocityAtPoint)
            if(velocityAtPoint.isEmpty || (velocityAtPoint.isDefined && velocityAtPoint.get.isUndefined)) {
              moveUpDepths(point)
            } else {
              Some(point)
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
    else {
      None
    }
  }

  private def moveUpDepths(point : GeoCoordinate) : Option[GeoCoordinate] = {
    var depth = point.depth - 5
    debug("Trying with depth " + depth)
    var newPoint : Option[GeoCoordinate] = None
    while(depth > 0 && !newPoint.isDefined) {
      info("Trying with depth " + depth)
      val newDepth = new GeoCoordinate(point.latitude, point.longitude, depth)
      flow.getVelocity(newDepth) match {
        case Some(velocity) => newPoint = Some(newDepth)
        case None => depth -= 5
      }
    }
    newPoint
  }

  private def performRungeKuttaIteration(coordinate: GeoCoordinate, velocity: Option[Velocity],
                                         partialTimeStep: Int, time: LocalDateTime, swimming: Option[Velocity]): RungeKuttaStepDerivative = {
    if (velocity.isDefined) {
      val normalisedTime = partialTimeStep - timeStep
      val newCoordinate = geometry.translatePoint(coordinate, velocity.get, partialTimeStep, swimming.getOrElse(new Velocity(0,0,0)))
      val newVelocity = flow.getVelocityOfCoordinate(newCoordinate, time.plusSeconds(normalisedTime), time, partialTimeStep)
      new RungeKuttaStepDerivative(newVelocity, newCoordinate)
    } else {
      new RungeKuttaStepDerivative(None, coordinate)
    }
  }
}

case class RungeKuttaStepDerivative(val velocity: Option[Velocity], val coordinate: GeoCoordinate)

package main

import biology._
import grizzled.slf4j._
import maths.integration.RungeKuttaIntegration

class ParticleDisperser(integrator: RungeKuttaIntegration) extends Logging {

  def updatePosition(larva: ReefFish, clock: SimulationClock): Unit = {
    updatePosition(larva, clock, 0)
  }

  def updatePosition(larva: ReefFish, clock: SimulationClock, swimmingSpeed: Double): Unit = {
    debug("The old position is " + larva.position)
    val position = integrator.integrate(larva.position, clock.now, swimmingSpeed)
    if (!position.isUndefined) larva.move(position)
    debug("The new position is " + position)
  }

}
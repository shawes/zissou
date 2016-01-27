package main

import biology._
import grizzled.slf4j._
import maths.integration.RungeKuttaIntegration

class ParticleDisperser(integrator: RungeKuttaIntegration) {

  val logger = Logger(classOf[ParticleDisperser])

  def updatePosition(larva: ReefFish, clock: SimulationClock): Unit = {
    updatePosition(larva, clock, 0)
  }

  def updatePosition(larva: ReefFish, clock: SimulationClock, swimmingSpeed: Double): Unit = {
    val position = integrator.integrate(larva.position, clock.now, swimmingSpeed)
    logger.debug("The old position is " + larva.position)
    larva.move(position)
    logger.debug("The new position is " + position)
  }

}
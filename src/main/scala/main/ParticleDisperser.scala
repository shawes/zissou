package main

import biology._
import grizzled.slf4j._
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import physical.GeoCoordinate

class ParticleDisperser(integrator: RungeKuttaIntegration, randomNumbers: RandomNumberGenerator) extends Logging {

  def updatePosition(larva: ReefFish, clock: SimulationClock): Unit = {
    updatePosition(larva, clock, 0)
  }

  def updatePosition(larva: ReefFish, clock: SimulationClock, swimmingSpeed: Double): Unit = {
    debug("The old position is " + larva.position)
    val migratedPositionVertically: GeoCoordinate = applyVerticalMigration(larva)

    var position = integrator.integrate(migratedPositionVertically, clock.now, swimmingSpeed)
    if (position.isUndefined) {
      position = integrator.integrate(larva.position, clock.now, swimmingSpeed)
      if (position.isUndefined) {
        position = larva.position // Don't move
      }
    }

    larva.move(position)
    debug("The new position is " + position)
  }

  private def applyVerticalMigration(larva: ReefFish): GeoCoordinate = {
    val depth = larva.getOntogeneticVerticalMigrationDepth(randomNumbers)
    new GeoCoordinate(larva.position.latitude, larva.position.longitude, depth)
  }

}
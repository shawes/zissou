package main

import biology._
import grizzled.slf4j._
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import physical.GeoCoordinate
import physical.habitat.HabitatManager

class ParticleDisperser(integrator: RungeKuttaIntegration, randomNumbers: RandomNumberGenerator) extends Logging {

  def updatePosition(larva: ReefFish, clock: SimulationClock, habitats: HabitatManager): Unit = {
    updatePosition(larva, clock, 0, habitats)
  }

  def updatePosition(larva: ReefFish, clock: SimulationClock, swimmingSpeed: Double, habitats: HabitatManager): Unit = {
    //debug("The old position is " + larva.position)
    val migratedPositionVertically: GeoCoordinate = applyVerticalMigration(larva)

    val position = integrator.integrate(migratedPositionVertically, clock.now, swimmingSpeed)
    //var count = 0
    //while (position.isUndefined && count < 3) {
    //position = integrator.integrate(applyVerticalMigration(larva), clock.now, swimmingSpeed)
    //count += 1
    //}

    if (position.isValid) {
      larva.move(position) //, habitats.getHabitatOfCoordinate(position))
    } else {
      // Stay in the same place and try next time
      warn("Couldn't move to position " + position + ", so staying at " + larva.position)
      larva.move(larva.position) //, habitats.getHabitatOfCoordinate(position))
    }

    //if (position.isValid && habitats.isOcean(position)) {
    //larva.move(position, habitats.getHabitatOfCoordinate(position))
    //}
    //else {
      // Don't move
      //larva.move(larva.position,habitats.getHabitatOfCoordinate(position))
    //}
    //debug("The new position is " + position)
  }

  private def applyVerticalMigration(larva: ReefFish): GeoCoordinate = {
    val depth = larva.getOntogeneticVerticalMigrationDepth(randomNumbers)
    new GeoCoordinate(larva.position.latitude, larva.position.longitude, depth)
  }

}
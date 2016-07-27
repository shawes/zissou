package main

import biology._
import grizzled.slf4j._
import maths.integration.RungeKuttaIntegration
import physical.GeoCoordinate
import physical.habitat.HabitatManager

class ParticleDisperser(integrator: RungeKuttaIntegration, ovm: Boolean) extends Logging {

  def updatePosition(larva: ReefFish, clock: SimulationClock, habitats: HabitatManager): Unit = {
    updatePosition(larva, clock, 0, habitats)
  }

  def updatePosition(larva: ReefFish, clock: SimulationClock, swimmingSpeed: Double, habitats: HabitatManager): Unit = {
    //debug("The old position is " + larva.position)

    val migratedPositionVertically: GeoCoordinate = if (ovm) migrateLarvaVertically(larva) else larva.position

    val position = integrator.integrate(migratedPositionVertically, clock.now, swimmingSpeed)
    //var count = 0
    //while (position.isUndefined && count < 3) {
    //position = integrator.integrate(migrateLarvaVertically(larva), clock.now, swimmingSpeed)
    //count += 1
    //}

    if (position.isDefined) {
      larva.move(position.get) //, habitats.getHabitatOfCoordinate(position))
    } else {
      // Stay in the same place and try next time
      debug("Couldn't move to position, so staying at " + larva.position)
      //larva.move(migratedPositionVertically) //, habitats.getHabitatOfCoordinate(position))
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

  private def migrateLarvaVertically(larva: ReefFish): GeoCoordinate =
    new GeoCoordinate(larva.position.latitude, larva.position.longitude, larva.getOntogeneticVerticalMigrationDepth)


}
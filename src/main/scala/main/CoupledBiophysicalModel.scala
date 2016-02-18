package main

import grizzled.slf4j.Logging
import io.ResultsWriter
import io.config.ConfigMappings._
import io.config.Configuration
import locals.VerticalMigrationPattern
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import physical.Turbulence
import physical.flow.Flow
import utilities.Timer

import scala.compat.Platform

/**
  *
  * Created by Steven Hawes on 27/01/2016.
  */
class CoupledBiophysicalModel(val config: Configuration) extends Logging {

  val flow: Flow = config.flow
  val clock = new SimulationClock(flow.period, flow.timeStep)
  val randomSeed: Long = Platform.currentTime
  info("The random number seed for this simulation is :" + randomSeed)
  val randomNumbers = new RandomNumberGenerator(randomSeed)
  val turbulence: Turbulence = new Turbulence(config.turbulence.horizontalDiffusionCoefficient,
    config.turbulence.verticalDiffusionCoefficient, flow.timeStep.totalSeconds, randomNumbers)
  val ocean = new PhysicalModel(config, randomNumbers)
  val biology = new BiologicalModel(config, clock, randomNumbers)
  val integrator = new RungeKuttaIntegration(ocean.flowController, turbulence, flow.timeStep.totalSeconds)
  val ovm = config.fish.verticalMigrationPattern == VerticalMigrationPattern.Ontogenetic.toString
  val larvaeDisperser = new ParticleDisperser(integrator, randomNumbers, ovm)

  def run(): Unit = {
    val simulationTimer = new Timer()
    info("Simulation run started at " + simulationTimer.start)
    var iteration: Int = 1
    ocean.initialise()

    //val coord= new GeoCoordinate(-30.54,152.99)
    //val vel = ocean.flowController.getVelocityOfCoordinate(coord,false)
    //info("Velocity is "+vel)
    val stepTimer = new Timer()
    while (clock.stillTime && biology.canDisperse(clock.now)) {
      if (clock.isMidnight) stepTimer.reset()
      biology.apply(iteration, larvaeDisperser)
      clock.tick()
      if (clock.isMidnight) {
        ocean.circulate()
        info("Day iteration " + iteration / 12 + " has been completed in " + stepTimer.stop() + " secs")
      }

      iteration = iteration + 1
    }

    val resultsWriter = new ResultsWriter(biology.fishLarvae.toList, config.output)
    resultsWriter.write()
    info("Simulation run completed in " + simulationTimer.stop() / 60 + " minutes")
  }
}

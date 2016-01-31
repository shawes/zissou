package main

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.ResultsWriter
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import org.joda.time.Duration
import physical.Turbulence
import physical.flow.Flow

import scala.compat.Platform

/**
  *
  * Created by Steven Hawes on 27/01/2016.
  */
class CoupledBiophysicalModel(val config: Configuration) extends Logging {

  val flow: Flow = config.flow
  val clock = new SimulationClock(flow.period, flow.timeStep)
  val randomSeed: Long = Platform.currentTime
  info("The random number seed is :" + randomSeed)
  val randomNumbers = new RandomNumberGenerator(randomSeed)
  val turbulence: Turbulence = new Turbulence(config.turbulence.horizontalDiffusionCoefficient,
    config.turbulence.verticalDiffusionCoefficient, flow.timeStep.totalSeconds, randomNumbers)
  val ocean = new PhysicalModel(config, randomNumbers)
  val biology = new BiologicalModel(config, clock, randomNumbers)
  val integrator = new RungeKuttaIntegration(ocean.flowController, turbulence, flow.timeStep.totalSeconds)
  val larvaeDisperser = new ParticleDisperser(integrator, randomNumbers)

  def run(): Unit = {
    val simulationStartTime = DateTime.now
    debug("Simulation run started at " + simulationStartTime)
    var iteration: Int = 1
    ocean.initialise()

    while (clock.stillTime && biology.canDisperse(clock.now)) {
      biology.apply(iteration, larvaeDisperser)
      clock.tick()
      if (clock.isMidnight) {
        ocean.circulate()
      }
      info("Step " + iteration + " has been completed")
      iteration = iteration + 1
    }

    val resultsWriter = new ResultsWriter(biology.fishLarvae.flatten.toList, config.output)
    resultsWriter.write()
    debug("Simulation run completed at " + new Duration(DateTime.now, clock.start).getStandardMinutes)
  }
}

package main

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logger
import io.ResultsWriter
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import org.joda.time.Duration
import physical.Turbulence
import physical.flow.Flow

/**
  *
  * Created by Steven Hawes on 27/01/2016.
  */
class CoupledBiophysicalModel(val config: Configuration) {

  val logger = Logger(classOf[CoupledBiophysicalModel])
  val flow: Flow = config.flow
  val clock = new SimulationClock(flow.period, flow.timeStep)
  val randomNumbers = new RandomNumberGenerator(666)
  val turbulence: Turbulence = new Turbulence(Math.pow((2 * turbulence.horizontalDiffusionCoefficient) / flow.timeStep.totalSeconds, 0.5),
    Math.pow((2 * turbulence.verticalDiffusionCoefficient) / flow.timeStep.totalSeconds, 0.5), randomNumbers)
  val ocean = new PhysicalModel(config, randomNumbers)
  val biology = new BiologicalModel(config, clock, randomNumbers)
  val larvaeDisperser = new ParticleDisperser(new RungeKuttaIntegration(ocean.flowController, turbulence, flow.timeStep.totalSeconds))

  def run(): Unit = {
    val simulationStartTime = DateTime.now
    logger.debug("Simulation run started at " + simulationStartTime)
    var iteration: Int = 1
    ocean.initialise()

    while (clock.stillTime && biology.canDisperse(clock.now)) {
      biology.apply(iteration, larvaeDisperser)
      clock.tick()
      if (clock.isMidnight) {
        ocean.circulate()
      }
      logger.info("Step " + iteration + " has been completed")
      iteration = iteration + 1
    }

    val resultsWriter = new ResultsWriter(biology.fishLarvae.flatten.toList, config.output)
    resultsWriter.write()
    logger.debug("Simulation run completed at " + new Duration(DateTime.now, clock.start).getStandardMinutes)
  }
}

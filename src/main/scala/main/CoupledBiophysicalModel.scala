package main

import grizzled.slf4j.Logging
import io.ResultsIO
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import physical.Turbulence
import physical.flow.Flow
import utilities.SimpleTimer


class CoupledBiophysicalModel(val config: Configuration) extends Logging {

  val flow: Flow = config.flow
  val clock = new SimulationClock(flow.period, flow.timeStep)

  val turbulence: Turbulence = new Turbulence(config.turbulence.horizontalDiffusionCoefficient,
    config.turbulence.verticalDiffusionCoefficient, flow.timeStep.totalSeconds, RandomNumberGenerator)
  val ocean = new PhysicalModel(config)
  val integrator = new RungeKuttaIntegration(ocean.flowController, turbulence, flow.timeStep.totalSeconds)
  val biology = new BiologicalModel(config, clock, integrator)

  def run(): Unit = {
    val simulationTimer = new SimpleTimer()
    simulationTimer.start()
    info("Simulation run started")
    //var iteration: Int = 1
    var iteration = 1
    ocean.initialise()
    val stepTimer = new SimpleTimer()
    stepTimer.start()
    while (clock.stillTime && biology.canDisperse(clock.now)) {
      biology(iteration)
      clock.tick()
      if (clock.isMidnight) {
        ocean.circulate()
        stepTimer.stop()
        info("Day iteration " + iteration / 12 + " has been completed in " + stepTimer.result() + " secs")
        stepTimer.start()
      }
      //iteration += 1
      iteration += 1
    }

    ocean.close()

    val resultsWriter = new ResultsIO(biology.fishLarvae.toList, config.output)
    resultsWriter.write()
    simulationTimer.stop()
    info("Simulation run completed in " + simulationTimer.result / 60 + " minutes")
  }
}

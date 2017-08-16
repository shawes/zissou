package model

import grizzled.slf4j.Logging
import io.ResultsIO
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import maths.integration.RungeKuttaIntegration
import physical.Turbulence
import physical.flow.Flow
import utilities.SimpleTimer


class CoupledBiophysicalModel(val config: Configuration, val name : String) extends Logging {

  val flow: Flow = config.flow
  val clock = new SimulationClock(flow.period, flow.timeStep)


  val turbulence: Option[Turbulence] = config.turbulence.applyTurbulence match {
      case true => Some(new Turbulence(config.turbulence.horizontalDiffusionCoefficient,
        config.turbulence.verticalDiffusionCoefficient, flow.timeStep.totalSeconds, RandomNumberGenerator))
      case false => None
    }
  val ocean = new PhysicalModel(config)
  val integrator = new RungeKuttaIntegration(ocean.flowController, turbulence, flow.timeStep.totalSeconds)
  val biology = new BiologicalModel(config, clock, integrator)

  def run(): Unit = {
    try {
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
        //stepTimer.stop()
        info("Day iteration " + iteration / 12 + " has been completed in " + stepTimer.stop() + " secs")
        stepTimer.start()
      }
      iteration += 1
    }
    info("Simulation run completed in " + (simulationTimer.stop() / 60.0) + " minutes")
    ocean.shutdown()

    val resultsWriter = new ResultsIO(biology.stationaryLarvae.toList, config.output, name)
    resultsWriter.write()

  } catch {
    case e : Exception => e.printStackTrace()
  } finally {
    ocean.shutdown()
  }
}
}

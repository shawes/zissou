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

class CoupledBiophysicalModel(val config: Configuration, val name: String)
    extends Logging {
  setConfiguredLogLevel()

  if (config.inputFiles.randomSeed.isValidInt) {
    RandomNumberGenerator.setSeed(config.inputFiles.randomSeed)
  }

  val flow: Flow = config.flow
  val clock = new SimulationClock(flow.period, flow.timeStep)

  val turbulence: Option[Turbulence] = config.turbulence.applyTurbulence match {
    case true =>
      Some(
        new Turbulence(
          config.turbulence.horizontalDiffusionCoefficient,
          config.turbulence.verticalDiffusionCoefficient,
          flow.timeStep.totalSeconds
        )
      )
    case false => None
  }

  val ocean = new PhysicalModel(config)
  val integrator = new RungeKuttaIntegration(
    ocean.flowController,
    turbulence,
    flow.timeStep.totalSeconds
  )
  val biology = new BiologicalModel(config, clock, integrator)

  def run(): Unit = {
    try {

      val simulationTimer = new SimpleTimer()
      simulationTimer.start()
      info("Simulation run started")
      val stepTimer = new SimpleTimer()
      stepTimer.start()
      while (clock.stillTime && biology.canDisperse(clock.now)) {
        biology()
        if (clock.isMidnight) {
          if (config.larva.isMortal) {
            biology.applyMortality()
          }
          info(
            "Day " + clock.now.toLocalDate + " has been completed in " + stepTimer
              .stop() + " secs with " + biology.pelagicLarvae.size + " larvae."
          )
          stepTimer.start()
          ocean.circulate()
        }
        clock.tick()
      }
      val time: Double = simulationTimer.stop() / 60.0
      info(f"Simulation run completed in $time%.2f minutes")
      val still = clock.stillTime
      val disperse = biology.canDisperse(clock.now)
      debug(s"Still movin': $still and dispersin': $disperse")
      ocean.shutdown()

      val resultsWriter =
        new ResultsIO(biology.stationaryLarvae.toArray, config.output, name)
      resultsWriter.write()

    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      ocean.shutdown()
    }
  }

  private def setConfiguredLogLevel(): Unit = {
    //val logFile = config.output.logFile
    if (config.output.logFile.nonEmpty) {
      System.setProperty(
        "org.slf4j.simpleLogger.logFile",
        config.output.logFile
      )
    }
    config.output.logLevel match {
      case "debug" =>
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
      case "trace" =>
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
      case "error" =>
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")
      case "off" =>
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off")
      case "all" =>
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "all")
      case _ =>
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info")
    }
  }

}

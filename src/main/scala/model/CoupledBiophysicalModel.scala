package model

import com.github.nscala_time.time.Imports.LocalDateTime
import exceptions.FlowDataExhaustedException
import grizzled.slf4j.Logging
import io._
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

  RandomNumberGenerator.setSeed(config.settings.randomSeed)

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
  val biology = new BiologicalModel(
    config,
    clock,
    integrator,
    if (config.larva.temperatureMortality.isDefined)
      Some((position: physical.GeoCoordinate, time: LocalDateTime) => ocean.flowController.getTemperature(position, time))
    else None
  )

  def run(): Unit = {
    try {

      val simulationTimer = new SimpleTimer()
      simulationTimer.start()
      info("Simulation run started")
      val stepTimer = new SimpleTimer()
      stepTimer.start()
      while (clock.stillTime && biology.isDispersing(clock.now) && flowReaches(clock.now)) {
        biology()
        if (clock.isFirstStepOfDay) {
          if (config.larva.isMortal.getOrElse(false)) {
            biology.applyMortality()
          }
          val waited = math.round(ocean.takeWaitSeconds())
          // what the run waited for the flow, apart from its own work, so that a slow day can be told from a slow network
          val waiting = if (waited > 0) s" (waited $waited secs for flow data)" else ""
          info(
            "Day " + clock.now.toLocalDate + " has been completed in " + stepTimer
              .stop() + " secs with " + biology.pelagicLarvae.size + " larvae" + waiting + "."
          )
          stepTimer.start()
        }
        clock.tick()
      }
      val time: Double = simulationTimer.stop() / 60.0
      info(f"Simulation run completed in $time%.2f minutes")
      biology.temperatureMortalitySummary.foreach { case (mean, checks, unknown) =>
        val reference = config.larva.mortalityRate.getOrElse(0.0)
        info(f"Mortality by temperature: ${mean * 100}%.1f%% of the larvae died a day on average, where ${reference * 100}%.1f%% is the rate at ${config.larva.temperatureMortality.get.referenceTemperature}%.1f degrees" +
          (if (unknown > 0) s"; there was no temperature for $unknown of $checks larvae, which were given the reference rate" else ""))
      }
      if (biology.larvaeLostFromDomain > 0) {
        warn(
          s"${biology.larvaeLostFromDomain} larvae left the edge of the flow grid and were killed"
        )
      }
      val still = clock.stillTime
      val disperse = biology.isDispersing(clock.now)
      debug(s"Still movin': $still and dispersin': $disperse")
      ocean.shutdown()

      // Larvae still drifting when the run ends, at the end of its period or of the flow
      // files, are written with the rest: their paths, to where they are now, are part of
      // the dispersal, though they have not settled
      val drifting = biology.pelagicLarvae
      if (drifting.nonEmpty) {
        info(s"${drifting.size} larvae were still drifting when the run ended")
        drifting.foreach(_.recordPosition())
      }
      val resultsWriter =
        new ResultsWriter((biology.stationaryLarvae ++ drifting).toArray, config.output, name)
      resultsWriter.write()

    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      ocean.shutdown()
    }
  }

  // False, with a warning, once the flow files have nothing for the time: the simulation
  // stops there and what it has done is kept
  private def flowReaches(time: LocalDateTime): Boolean =
    try {
      ocean.update(time)
      true
    } catch {
      case e: FlowDataExhaustedException =>
        warn(s"${e.getMessage} The simulation stops at $time, before the end of its period.")
        false
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

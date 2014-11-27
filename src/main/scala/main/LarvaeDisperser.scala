package main

import java.io.File

import biology.{Mortality, ReefFish, Spawn, TheMaker}
import com.github.nscala_time.time.Imports._
import io.config.ConfigMappings._
import io.{FlowReader, Logger}
import org.apache.commons.math.random.MersenneTwister
import org.joda.time.Days
import physical.Turbulence
import physical.flow.FlowController
import physical.habitat.HabitatManager

import scala.compat.Platform


class LarvaeDisperser(params: io.config.Configuration) {
  require(params != null)

  val god = new TheMaker(params.fish, false)
  val mortality = new Mortality(params.fish.pelagicLarvalDuration.mean)
  val random = new MersenneTwister(Platform.currentTime)
  val flowController = new FlowController(params.flow)
  val spawn = new Spawn(params.spawn)
  val flowDataReader = new FlowReader(params.inputFiles, params.flow.depth)
  val startTime: DateTime = new DateTime(flowController.flow.period.start)
  val finishTime: DateTime = new DateTime(flowController.flow.period.end)
  val totalDays: Int = Days.daysBetween(startTime, finishTime).getDays
  val timeStep = flowController.flow.timeStep.totalSeconds
  val turbulence: Turbulence = new Turbulence(Math.pow((2 * params.turbulence.horizontalDiffusionCoefficient) / timeStep, 0.5),
    Math.pow((2 * params.turbulence.verticalDiffusionCoefficient) / timeStep, 0.5))
  var habitatFile = new File(params.inputFiles.habitatFilePath)
  var habitatMgr: HabitatManager = new HabitatManager(habitatFile, params.habitat.buffer, Array("Reef", "Other"))
  var fishLarvae: Vector[Array[ReefFish]] = Vector.empty
  var currentTime = startTime
  var pelagicLarvaeCount = 0
  var startRun: DateTime = null


  def run(): Unit = {
    startRun = DateTime.now
    Logger.info("Simulation run started at " + startTime)
    var iteration: Int = 1
    while (currentTime <= finishTime && (spawn.isItSpawningSeason(currentTime) || pelagicLarvaeCount > 0)) {
      currentTime = currentTime.plusSeconds(timeStep)
      if (currentTime.getHourOfDay == 0) readNextFlowTimeStep()


      iteration += 1
      Logger.info("Time is now " + currentTime)
    }
  }


  def readNextFlowTimeStep() = if (flowDataReader.hasNext) flowController.refresh(flowDataReader.next())

  def readInitialFlowData() = {
    flowController.initialiseFlow(flowDataReader)
    Logger.info("There are this many polygons " + flowController.flowDataQueue.head.length)
    Logger.info("There are this many days loaded " + flowController.flowDataQueue.length)
  }

  private def calculateMortalityRate(i: Int) = mortality.calculateMortalityRate(i)




}

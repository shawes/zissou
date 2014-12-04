package main

import java.io.File

import biology._
import com.github.nscala_time.time.Imports._
import io.config.ConfigMappings._
import io.{FlowReader, Logger}
import maths.integration.RungeKuttaIntegration
import org.apache.commons.math.random.MersenneTwister
import org.joda.time.Days
import physical.Turbulence
import physical.flow.FlowController
import physical.habitat.HabitatManager

import scala.collection.mutable
import scala.compat.Platform


class LarvaeDisperser(params: io.config.Configuration) {
  require(params != null)

  val god = new TheMaker(params.fish, false)
  val mortality = new Mortality(params.fish.pelagicLarvalDuration.mean)
  val random = new MersenneTwister(Platform.currentTime)
  val fish: Fish = params.fish
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

      calculateMortalityRate(iteration)
      spawnLarvae()
      iteration += 1
      Logger.info("Time is now " + currentTime)
    }
  }


  def readNextFlowTimeStep() = if (flowDataReader.hasNext) flowController.refresh(flowDataReader.next())

  private def calculateMortalityRate(iteration: Int) = mortality.calculateMortalityRate(iteration)

  private def spawnLarvae() = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(currentTime)
    if (spawningSites.nonEmpty) spawnFish(spawningSites)
  }

  private def spawnFish(sites: mutable.Buffer[SpawningLocation]) = {
    val freshLarvae = god.create(sites.toList)
    freshLarvae.foreach(x => fishLarvae :+ x)
    pelagicLarvaeCount += freshLarvae.size
  }

  def readInitialFlowData() = {
    flowController.initialiseFlow(flowDataReader)
    Logger.info("There are this many polygons " + flowController.flowDataQueue.head.length)
    Logger.info("There are this many days loaded " + flowController.flowDataQueue.length)
  }

  private def cycleThroughLarvae() = {
    val rungeKuttaIntegrator = new RungeKuttaIntegration(flowController, timeStep)
    fishLarvae.foreach(x => moveLarvae(x, rungeKuttaIntegrator))
  }

  private def moveLarvae(larvae: Array[ReefFish], iterator: RungeKuttaIntegration) {
    val swimmingLarvae = larvae.filter(x => x.canMove && x != null)

    for (larva <- swimmingLarvae) {

      if (fish.isMortal && random.nextDouble() < mortality.getRate) larva.kill()

      val speed: Double = if (fish.canSwim) fish.swimmingSpeed else 0
      val position = iterator.integrate(larva.currentPosition, currentTime, speed)
      larva.move(position)
      larva.age += timeStep
    }





    //            if (larva.CanSettle())
    //            {
    //              int indexOfReef;
    //              if (habitatManager.IsCoordinateOverReef(larva.Position, out indexOfReef))
    //              {
    //                if (indexOfNearestReef != Constants.NoClosestReefFound)
    //                  larva.Recruit(habitatManager.GetReef(indexOfReef), currentTime);
    //              }
    //              else if (habitat.Buffer.IsBuffered && habitatManager.IsCoordinateOverBuffer(larva.Position))
    //              {
    //                var indexOfNearestReef = habitatManager.GetIndexOfNearestReef(larva.Position);
    //                if (indexOfNearestReef != Constants.NoClosestReefFound)
    //                  larva.Recruit(habitatManager.GetReef(indexOfNearestReef), currentTime);
    //              }
    //              else if (larva.ReachedMaximum()) larva.Kill();
    //            }
    //            if (larva.State == LarvaState.Dead || larva.State == LarvaState.Recruited) pelagicLarveCount--;
    //          }
    //        }
  }


}

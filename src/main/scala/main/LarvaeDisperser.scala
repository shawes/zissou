package main

import java.io.File

import biology._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j._
import io.config.ConfigMappings._
import io.{DispersalKernelFileWriter, FlowReader, LarvaeFileWriter, ShapeFileWriter}
import locals.{Constants, PelagicLarvaeState, ShapeFileType}
import maths.integration.RungeKuttaIntegration
import org.apache.commons.math3.random.MersenneTwister
import org.joda.time.Days
import physical.Turbulence
import physical.flow.FlowController
import physical.habitat.HabitatManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.compat.Platform


class LarvaeDisperser(params: io.config.Configuration) {
  require(params != null)

  val god = new TheMaker(params.fish, false)
  val mortality = new MortalityDecay(params.fish.pelagicLarvalDuration.mean)
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
  val logger = Logger(classOf[LarvaeDisperser])
  var habitatFile = new File(params.inputFiles.habitatFilePath)
  var habitatMgr: HabitatManager = new HabitatManager(habitatFile, params.habitat.buffer, Array("Reef", "Other"))
  var fishLarvae: ListBuffer[List[ReefFish]] = ListBuffer.empty
  var currentTime = startTime
  var pelagicLarvaeCount = 0
  var startRun: DateTime = null
  var output = params.output

  def run(): Unit = {
    startRun = DateTime.now
    logger.debug("Simulation run started at " + startTime)
    var iteration: Int = 1
    readInitialFlowData()

    while (currentTime <= finishTime && (spawn.isItSpawningSeason(currentTime) || pelagicLarvaeCount > 0)) {
      calculateMortalityRate(iteration)
      spawnLarvae()
      cycleThroughLarvae()
      incrementTime()
      if (currentTime.getHourOfDay == 0) {
        readNextFlowTimeStep()
      }


      logger.info("Step " + iteration + " has been completed")
      iteration = iteration + 1
    }

    writeOutput()
    logger.debug("Simulation run completed at " + new Duration(DateTime.now, startTime).getStandardMinutes)
  }

  private def writeOutput() = {
    val larvaeList = fishLarvae.flatten.toList

    writeLarvaeMovementsToShapeFile(larvaeList)
    writeDispersalKernel(larvaeList)
    if (output.includeLarvaeHistory) writeLarvaeStateChangesToExcelFile(larvaeList)
  }


  private def writeLarvaeStateChangesToExcelFile(larvae: List[Larva]) = {
    val larvaeFileWriter = new LarvaeFileWriter()
    //larvaeFileWriter.writeExcelFile(fishLarvae.flatten, output.saveOutputFilePath)
  }

  private def writeDispersalKernel(larvae: List[Larva]) = {
    val dispersalKernelWriter = new DispersalKernelFileWriter(output.saveOutputFilePath, larvae)
    dispersalKernelWriter.writeDispersalKernelToCsv()
  }

  private def writeLarvaeMovementsToShapeFile(larvae: List[Larva]) = {
    val shapeFileWriter = new ShapeFileWriter(larvae, ShapeFileType.Line)
    //shapeFileWriter.writeShapes(larvae, output.SaveOutputFilePath, output.ShapeType);
  }


  def readNextFlowTimeStep(): Unit = {
    logger.debug("Reading next flow step")
    if (flowDataReader.hasNext) {
      logger.debug("Refreshing next flow step")
      flowController.refresh(flowDataReader.next())
    }
  }

  //def readInitialFlowData() = flowController.initialiseFlow(flowDataReader)

  private def calculateMortalityRate(iteration: Int) = mortality.calculateMortalityRate(iteration)

  private def spawnLarvae() = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(currentTime)
    if (spawningSites.nonEmpty) {
      logger.debug("Found non-empty spawning site")
      spawnFish(spawningSites)
    }
  }

  private def spawnFish(sites: mutable.Buffer[SpawningLocation]) = {
    val freshLarvae = god.create(sites.toList)
    freshLarvae.foreach(x => fishLarvae :: x)
    for (spawned <- freshLarvae) {
      fishLarvae += spawned.asInstanceOf[List[ReefFish]]
    }
    logger.debug("Now spawned " + fishLarvae.size + " fish larvae")
  }

  def readInitialFlowData() = {
    flowController.initialiseFlow(flowDataReader)
    logger.debug("There are this many polygons " + flowController.flowDataQueue.head.length)
    logger.debug("There are this many days loaded " + flowController.flowDataQueue.length)
  }

  private def cycleThroughLarvae() = {
    val rungeKuttaIntegrator = new RungeKuttaIntegration(flowController, timeStep)
    fishLarvae.foreach(x => moveLarvae(x, rungeKuttaIntegrator))
  }

  private def moveLarvae(larvae: List[ReefFish], iterator: RungeKuttaIntegration) {
    val swimmingLarvae = larvae.filter(x => x.canMove && x != null)


    for (larva <- swimmingLarvae) {

      mortalityCheck(larva)
      updatePosition(iterator, larva)
      larva.age += timeStep
      settle(larva)
    }

    //                int indexOfReef;
    //                if (habitatManager.IsCoordinateOverReef(larva.Position, out indexOfReef))
    //                {
    //                  if (indexOfNearestReef != Constants.NoClosestReefFound)
    //                    larva.Recruit(habitatManager.GetReef(indexOfReef), currentTime);
    //                }
    //                else if (habitat.Buffer.IsBuffered && habitatManager.IsCoordinateOverBuffer(larva.Position))
    //                {
    //                  var indexOfNearestReef = habitatManager.GetIndexOfNearestReef(larva.Position);
    //                  if (indexOfNearestReef != Constants.NoClosestReefFound)
    //                    larva.Recruit(habitatManager.GetReef(indexOfNearestReef), currentTime);
    //                }
    //                else if (larva.ReachedMaximum()) larva.Kill();
    //              }
    //              if (larva.State == LarvaState.Dead || larva.State == LarvaState.Recruited) pelagicLarveCount--;
    //
    //          }


  }

  private def settle(larva: ReefFish): Unit = {
    if (larva.attainedPld) {
      logger.debug("Reach its pld")
      val reefIndex = habitatMgr.isCoordinateOverReef(larva.position)
      if (reefIndex != Constants.NoClosestReefFound) {
        logger.debug("Found reef")
        larva.settle(habitatMgr.getReef(reefIndex), currentTime)
      } // else if() TODO: Implement the buffer
      else if (larva.attainedMaximumLifeSpan) larva.kill()

      if (larva.state == PelagicLarvaeState.Dead || larva.state == PelagicLarvaeState.Settled) pelagicLarvaeCount -= 1

    }
  }

  private def updatePosition(iterator: RungeKuttaIntegration, larva: ReefFish): Unit = {
    val speed: Double = if (fish.canSwim) fish.swimmingSpeed else 0
    val position = iterator.integrate(larva.position, currentTime, speed)
    logger.debug("The old position is " + larva.position)
    larva.move(position)
    logger.debug("The new position is " + position)
  }

  private def mortalityCheck(larva: ReefFish): Unit = {
    if (fish.isMortal && random.nextDouble() < mortality.getRate) {
      logger.debug("Killing larvae " + larva.id)
      larva.kill()
    }
  }

  private def incrementTime() = currentTime = currentTime.plusSeconds(timeStep)

  private def calculateLarvae: Int = fishLarvae.flatten.size

}

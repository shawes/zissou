package main

import java.io.File

import biology._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logger
import io.config.ConfigMappings._
import io.config.Configuration
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.{Constants, PelagicLarvaeState}
import maths.RandomNumberGenerator
import physical.habitat.HabitatManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  *
  * Created by Steven Hawes on 27/01/2016.
  */
class BiologicalModel(val config: Configuration, clock: SimulationClock, randomNumbers: RandomNumberGenerator) {

  val god = new TheMaker(config.fish.pelagicLarvalDuration, false)
  val mortality = new MortalityDecay(config.fish.pelagicLarvalDuration.mean)
  val fish: Fish = config.fish
  val spawn = new Spawn(config.spawn)
  val logger = Logger(classOf[BiologicalModel])
  var habitatManager: HabitatManager = new HabitatManager(new File(config.inputFiles.habitatFilePath), config.habitat.buffer, Array("Reef", "Other"))
  var fishLarvae: ListBuffer[List[ReefFish]] = ListBuffer.empty
  var pelagicLarvaeCount = 0
  //var startRun: DateTime = null

  def apply(iteration: Int, disperser: ParticleDisperser): Unit = {
    calculateMortalityRate(iteration)
    spawnLarvae()
    fishLarvae.foreach(fish => processLarva(fish, disperser))
  }

  private def processLarva(larvae: List[ReefFish], disperser: ParticleDisperser): Unit = {

    val swimmingLarvae: List[ReefFish] = larvae.filter(x => x.canMove)

    for (larva <- swimmingLarvae) {
      mortalityCheck(larva)
      move(disperser, larva)
      ageLarvae(larva)
      killCheck(larva)
      settle(larva)
      updateActiveLarvaeCount(larva.state)
    }
  }

  private def move(disperser: ParticleDisperser, larva: ReefFish): Unit = {
    if (fish.canSwim) {
      disperser.updatePosition(larva, clock, fish.swimmingSpeed)
    } else {
      disperser.updatePosition(larva, clock)
    }
  }

  private def updateActiveLarvaeCount(state: PelagicLarvaeState): Unit = {
    if (state == PelagicLarvaeState.Dead || state == PelagicLarvaeState.Settled) {
      pelagicLarvaeCount -= 1
    }
  }

  private def ageLarvae(larva: ReefFish): Unit = {
    larva.age += clock.step.totalSeconds
  }

  private def killCheck(larva: ReefFish): Unit = {
    if (larva.attainedMaximumLifeSpan) larva.kill()
  }

  private def settle(larva: ReefFish): Unit = {
    if (larva.attainedPld) {
      larva.kill()
    } else {
      logger.debug("Reach its pld")
      val reefIndex = habitatManager.isCoordinateOverReef(larva.position)
      if (reefIndex != Constants.NoClosestReefFound) {
        logger.debug("Found reef")
        larva.settle(habitatManager.getReef(reefIndex), clock.now)
      } else if (habitatManager.isBuffered && habitatManager.isCoordinateOverBuffer(larva.position)) {
        val reefIndex = habitatManager.getIndexOfNearestReef(larva.position)
        if (reefIndex != Constants.NoClosestReefFound) {
          logger.debug("Found buffer")
          larva.settle(habitatManager.getReef(reefIndex), clock.now)
        }
      }
    }
  }

  private def mortalityCheck(larva: ReefFish): Unit = {
    if (fish.isMortal && randomNumbers.get < mortality.getRate) {
      logger.debug("Killing larvae " + larva.id)
      larva.kill()
    }
  }

  private def calculateMortalityRate(iteration: Int) = mortality.calculateMortalityRate(iteration)

  private def spawnLarvae(): Unit = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(clock.now)
    if (spawningSites.nonEmpty) {
      logger.debug("Found non-empty spawning site")
      spawnFish(spawningSites)
    }
  }

  private def spawnFish(sites: mutable.Buffer[SpawningLocation]) = {
    val freshLarvae = god.create(sites.toList)
    pelagicLarvaeCount += freshLarvae.size
    freshLarvae.foreach(x => fishLarvae :: x)
    for (spawned <- freshLarvae) {
      fishLarvae += spawned.asInstanceOf[List[ReefFish]]
    }
    logger.debug("Now spawned " + fishLarvae.size + " fish larvae")
  }

  def canDisperse(time: DateTime): Boolean = spawn.isItSpawningSeason(time) || pelagicLarvaeCount > 0
}
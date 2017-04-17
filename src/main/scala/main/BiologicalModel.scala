package main

import java.io.File

import scala.collection.mutable.ListBuffer

import biology._
import biology.fish._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import physical.habitat.HabitatManager
import locals.LarvaType

class BiologicalModel(val config: Configuration, clock: SimulationClock) extends Logging {

  val factory = LarvaFactory.apply(LarvaType.Fish, config.fish)
  val mortality = new MortalityDecay(config.fish.pelagicLarvalDuration.mean)
  val fish: FishParameters = config.fish
  val spawn = new Spawn(config.spawn)
  var habitatManager: HabitatManager = new HabitatManager(new File(config.inputFiles.habitatFilePath), config.habitat.buffer, Array("Reef", "Other"))
  var fishLarvae: ListBuffer[List[Larva]] = ListBuffer.empty
  var pelagicLarvaeCount = 0

  def apply(iteration: Int, disperser: ParticleDisperser): Unit = {
    debug("Applying biology")
    calculateMortalityRate(iteration)
    spawnLarvae()
    fishLarvae.foreach(fish => processLarva(fish, disperser))
  }

  private def processLarva(larvae: List[Larva], disperser: ParticleDisperser): Unit = {
    debug("Processing the fish")
    val swimmingLarvae: List[Larva] = larvae.filter(fish => fish.isPelagic)
    debug(larvae.size + " larvae of which these can move: " + swimmingLarvae.size)
    swimmingLarvae.foreach(reefFish => apply(reefFish, disperser))

  }

  private def apply(larva: Larva, disperser: ParticleDisperser): Unit = {
    move(disperser, larva)
    ageLarvae(larva)
    settle(larva)
    lifespanCheck(larva)
    mortalityCheck(larva)
  }

  private def move(disperser: ParticleDisperser, larva: Larva): Unit = {
    debug("Original position " + larva.position)
    if (fish.canSwim) {
      disperser.updatePosition(larva, clock, fish.swimmingSpeed, habitatManager)
    } else {
      disperser.updatePosition(larva, clock, habitatManager)
    }
    debug("New position " + larva.position)
  }

  private def ageLarvae(larva: Larva): Unit = {
    larva growOlder clock.step.totalSeconds
  }

  private def mortalityCheck(larva: Larva): Unit = {
    if (fish.isMortal && RandomNumberGenerator.get < mortality.getRate) {
      larva.kill()
      pelagicLarvaeCount -= 1
    }
  }

  private def settle(larva: Larva): Unit = {
    if (larva.inCompetencyWindow) {
      debug("Larva " + larva.id + " is in the competency window now")
      if (habitatManager.isBuffered) {
        debug("Searching buffered reefs")
        val reefIndex = habitatManager.isCoordinateOverBufferLazy(larva.position)
        // = habitatManager.getIndexOfNearestReef(larva.position)
        if (reefIndex.isDefined) {
          debug("Larva is within reef buffer")
          larva.settle(habitatManager.getReef(reefIndex.get), clock.now)
          pelagicLarvaeCount -= 1
        } else {
          val distanceIndex = habitatManager.getIndexOfNearestReef(larva.position)
          val reef = habitatManager.getReef(distanceIndex)
          debug("Closest reef is still " + reef.distance(larva.position) * 100 + "km away")
        }
      }
      else {
        val reefIndex = habitatManager.isCoordinateOverReef(larva.position)
        if (reefIndex.isDefined) {
          larva.settle(habitatManager.getReef(reefIndex.get), clock.now)
          pelagicLarvaeCount -= 1
        }
      }
    }
  }

  private def lifespanCheck(larva: Larva): Unit = {
    if (larva.isTooOld) {
      larva.kill()
      pelagicLarvaeCount -= 1
    }
  }

  private def calculateMortalityRate(iteration: Int) = mortality.calculateMortalityRate(iteration)

  private def spawnLarvae(): Unit = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(clock.now)
    if (spawningSites.nonEmpty) {
      trace("Found non-empty spawning site")
      spawnFish(spawningSites)
    }
  }

  private def spawnFish(sites: List[SpawningLocation]) = {
    val freshLarvae = sites.map(site => factory.create(site, clock.now))
    pelagicLarvaeCount += freshLarvae.flatten.size
    fishLarvae ++= freshLarvae
  }

  def canDisperse(time: DateTime): Boolean = spawn.isItSpawningSeason(time) || pelagicLarvaeCount > 0

  private def updateActiveLarvaeCount(larva: Larva): Unit = {
    if (!larva.isPelagic) {
      pelagicLarvaeCount -= 1
    }
  }
}

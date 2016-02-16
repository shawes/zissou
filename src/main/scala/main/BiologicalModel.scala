package main

import java.io.File

import biology._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
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
class BiologicalModel(val config: Configuration, clock: SimulationClock, randomNumbers: RandomNumberGenerator) extends Logging {

  val god = new FishFactory(config.fish, false)
  val mortality = new MortalityDecay(config.fish.pelagicLarvalDuration.mean)
  val fish: Fish = config.fish
  val spawn = new Spawn(config.spawn)
  //val logger = Logger(classOf[BiologicalModel])
  var habitatManager: HabitatManager = new HabitatManager(new File(config.inputFiles.habitatFilePath), config.habitat.buffer, Array("Reef", "Other"))
  var fishLarvae: ListBuffer[List[ReefFish]] = ListBuffer.empty
  var pelagicLarvaeCount = 0
  //var startRun: DateTime = null
  var processed = 0

  def apply(iteration: Int, disperser: ParticleDisperser): Unit = {
    calculateMortalityRate(iteration)
    spawnLarvae()
    fishLarvae.par.foreach(fish => processLarva(fish, disperser))
  }

  private def processLarva(larvae: List[ReefFish], disperser: ParticleDisperser): Unit = {

    val swimmingLarvae: List[ReefFish] = larvae.filter(fish => fish.isPelagic)
    debug(larvae.size + " larvae of which these can move: " + swimmingLarvae.size)

    for (larva <- swimmingLarvae) {
      mortalityCheck(larva)
      if (larva.isPelagic) {
        move(disperser, larva)
        ageLarvae(larva)
        settle(larva)
        lifespanCheck(larva)
      }
      updateActiveLarvaeCount(larva.state)
    }
    processed += 1
    debug("Processed the larvae")
  }

  private def move(disperser: ParticleDisperser, larva: ReefFish): Unit = {
    if (fish.canSwim) {
      disperser.updatePosition(larva, clock, fish.swimmingSpeed, habitatManager)
    } else {
      disperser.updatePosition(larva, clock, habitatManager)
    }
  }

  private def updateActiveLarvaeCount(state: PelagicLarvaeState): Unit = {
    if (state == PelagicLarvaeState.Dead || state == PelagicLarvaeState.Settled) {
      pelagicLarvaeCount -= 1
    }
  }

  private def ageLarvae(larva: ReefFish): Unit = {
    larva growOlder clock.step.totalSeconds
  }

  private def mortalityCheck(larva: ReefFish): Unit = {
    if (fish.isMortal && randomNumbers.get < mortality.getRate) {
      larva.kill()
    }
  }

  private def settle(larva: ReefFish): Unit = {
    if (larva.inCompetencyWindow) {
      if (habitatManager.isBuffered && habitatManager.isCoordinateOverBuffer(larva.position)) {
        val reefIndex = habitatManager.getIndexOfNearestReef(larva.position)
        if (reefIndex != Constants.LightWeightException.NoReefFoundException) {
          trace("Larva is within reef buffer")
          larva.settle(habitatManager.getReef(reefIndex), clock.now)
        }
      }
      else {
        val reefIndex = habitatManager.isCoordinateOverReef(larva.position)
        if (reefIndex != Constants.LightWeightException.NoReefFoundException) {
          larva.settle(habitatManager.getReef(reefIndex), clock.now)
        }
      }
    }
  }

  private def lifespanCheck(larva: ReefFish): Unit = {
    if (larva.isTooOld) larva.kill()
  }

  private def calculateMortalityRate(iteration: Int) = mortality.calculateMortalityRate(iteration)

  private def spawnLarvae(): Unit = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(clock.now)
    if (spawningSites.nonEmpty) {
      trace("Found non-empty spawning site")
      spawnFish(spawningSites)
    }
  }

  private def spawnFish(sites: mutable.Buffer[SpawningLocation]) = {
    val freshLarvae = god.createReefFish(sites.toList, clock.now)
    pelagicLarvaeCount += freshLarvae.size
    freshLarvae.foreach(x => fishLarvae :: x)
    for (spawned <- freshLarvae) {
      fishLarvae += spawned.asInstanceOf[List[ReefFish]]
    }
  }

  def canDisperse(time: DateTime): Boolean = spawn.isItSpawningSeason(time) || pelagicLarvaeCount > 0
}
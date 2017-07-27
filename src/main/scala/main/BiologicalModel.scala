package main

import java.io.File

import scala.collection.mutable.ListBuffer

import biology._
import biology.fish._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.config.ConfigMappings._
import io.config.Configuration
import locals.{DielVerticalMigrationType, LarvaType}
import maths.{Geometry, RandomNumberGenerator}
import maths.integration.RungeKuttaIntegration
import physical.Velocity
import physical.habitat.HabitatManager

class BiologicalModel(val config: Configuration, clock: SimulationClock, integrator: RungeKuttaIntegration) extends Logging {

  val fish: FishParameters = config.fish
  val factory = LarvaFactory.apply(LarvaType.Fish, config.fish)
  val mortality = new MortalityDecay(config.fish.pelagicLarvalDuration.mean)
  val spawn = new Spawn(config.spawn)
  var habitatManager: HabitatManager = new HabitatManager(new File(config.inputFiles.habitatFilePath), config.habitat.buffer, Array("Reef", "Other"))
  var fishLarvae: ListBuffer[List[Larva]] = ListBuffer.empty
  var pelagicLarvaeCount = 0
  val geometry = new Geometry


  def apply(iteration: Int): Unit = {
    debug("Applying biology")
    calculateMortalityRate(iteration)
    spawnLarvae()
    fishLarvae.foreach(fish => processLarva(fish))
  }

  private def processLarva(larvae: List[Larva]): Unit = {
    debug("Processing the fish")
    val swimmingLarvae: List[Larva] = larvae.filter(fish => fish.isPelagic)
    debug(larvae.size + " larvae of which these can move: " + swimmingLarvae.size)
    swimmingLarvae.foreach(reefFish => apply(reefFish))
  }

  private def apply(larva: Larva): Unit = {
    move(larva)
    ageLarvae(larva)
    settle(larva)
    lifespanCheck(larva)
    mortalityCheck(larva)
  }

  private def move(larva: Larva): Unit = {
    debug("Old position " + larva.position)
    val dampeningFactor: List[Double] = List(1.0, 0.66, 0.33)
    val orientate = swim(larva)
    def moveParticle(larva : Larva, dampeningFactor : List[Double]) : Unit = {
      debug("Dampening factor size: " + dampeningFactor.size)
      if(dampeningFactor.nonEmpty) {
        integrator.integrate(larva.position, clock.now, orientate, dampeningFactor.head) match {
          case Some(position) => larva.move(position)
          case None => moveParticle(larva, dampeningFactor.tail)//debug("Larvae could not move")
        }
      }
      else {
        debug("Larvae could not move")
        larva.move(larva.position)
      }
    }
    moveParticle(larva, dampeningFactor)
    migrateLarvaVertically(larva)
    debug("New position " + larva.position)
  }

  private def swim(larva : Larva) : Option[Velocity] = {
    if(larva.swimming.isDirected) {
        habitatManager.isCoordinateOverBufferLazy(larva.position, isSettlement=false) match {
          case Some(reefIndex) => Some(orientateTowardsReef(larva, reefIndex))
          case None => None
        }
    } else {
      None
    }
  }

  private def orientateTowardsReef(larva : Larva, reefIndex : Int) : Velocity = {
    val reef = habitatManager.getReef(reefIndex)
    val angle = geometry.getAngleBetweenTwoPoints(larva.position, reef.centroid)
    larva.swimming(angle)
  }

  private def migrateLarvaVertically(larva: Larva): Unit = {
    if(larva.undergoesDielMigration) {
      debug("Diel migration")
      if(clock.isSunRising(larva.position, "Australia/Sydney")) {
        debug("Rising sun")
        larva.dielVerticallyMigrate(DielVerticalMigrationType.Day)
      } else if(clock.isSunSetting(larva.position, "Australia/Sydney")) {
        debug("Setting sun")
        larva.dielVerticallyMigrate(DielVerticalMigrationType.Night)
      }
    }
    if(larva.undergoesOntogeneticMigration) {
      debug("OVM migration")
      larva.ontogeneticVerticallyMigrate
    }
  }

  private def ageLarvae(larva: Larva): Unit = {
    larva growOlder clock.timeStep.totalSeconds
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
        val reefIndex = habitatManager.isCoordinateOverBufferLazy(larva.position, isSettlement=true)
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

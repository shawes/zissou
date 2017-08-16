package model

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
  //var fishLarvae: ListBuffer[Larva] = ListBuffer.empty
  val pelagicLarvae: ListBuffer[Larva] = ListBuffer.empty
  val stationaryLarvae: ListBuffer[Larva] = ListBuffer.empty
  //val settledLarvae: ListBuffer[Larva] = ListBuffer.empty
  //var pelagicLarvaeCount = 0
  val geometry = new Geometry


  def apply(iteration: Int): Unit = {
    //debug("Applying biology")
    calculateMortalityRate(iteration)
    pelagicLarvae.par.foreach(fish => mortality(fish))
    refresh()
    spawnLarvae()
    pelagicLarvae.par.foreach(fish => biology(fish))
  }

  def refresh() : Unit = {
    val cull = pelagicLarvae.partition(larva => larva.isPelagic)
    pelagicLarvae.clear()
    pelagicLarvae ++= cull._1
    stationaryLarvae ++= cull._2
  }

  private def biology(larva: Larva): Unit = {
    ageLarvae(larva)
    move(larva)
    sense(larva)
    lifespanCheck(larva)
  }

  private def move(larva: Larva): Unit = {
    val dampeningFactor: List[Double] = List(1.0)
    val orientate = swim(larva)
    def moveParticle(larva : Larva, dampeningFactor : List[Double]) : Unit = {
      if(dampeningFactor.nonEmpty) {
        integrator.integrate(larva.position, clock.now, orientate, dampeningFactor.head) match {
          case Some(newPosition) => larva.move(newPosition)
          case None => moveParticle(larva, dampeningFactor.tail)
        }
      } else {
        larva.move(larva.position)
      }
    }
    moveParticle(larva, dampeningFactor)
    migrateLarvaVertically(larva)
  }

  private def swim(larva : Larva) : Option[Velocity] = {
    if(larva.swimming.isDirected && larva.direction != -1) {
      Some(larva.swimming(larva.direction))
    } else {
      None
    }
  }



  private def migrateLarvaVertically(larva: Larva): Unit = {
    if(larva.undergoesDielMigration) {
      if(clock.isSunRising(larva.position, "Australia/Sydney")) {
        larva.dielVerticallyMigrate(DielVerticalMigrationType.Day)
      } else if(clock.isSunSetting(larva.position, "Australia/Sydney")) {
        larva.dielVerticallyMigrate(DielVerticalMigrationType.Night)
      }
    }
    if(larva.undergoesOntogeneticMigration && larva.changedOntogeneticState) {
      larva.ontogeneticVerticallyMigrate
    }
  }

  private def ageLarvae(larva: Larva): Unit = {
    larva growOlder clock.timeStep.totalSeconds
  }

  private def mortality(larva: Larva): Unit = {
    if (fish.isMortal && RandomNumberGenerator.get < mortality.getRate) {
      larva.kill()
    }
  }

  private def sense(larva : Larva) : Unit = {
    if(larva.inCompetencyWindow) {
      val index = habitatManager.getClosestHabitat2(larva.position)
      if(index._1 != -1)  {
        larva.settle(index._1, clock.now)
      } else {
        if(index._2 != -1) {
          larva.changeDirection(index._3)
        } else {
          larva.changeDirection(-1)
        }
      }
    }
  }

  private def lifespanCheck(larva: Larva): Unit = {
    if (larva.isTooOld) {
      larva.kill()
    }
  }

  private def calculateMortalityRate(iteration: Int) = mortality.calculateMortalityRate(iteration)

  private def spawnLarvae(): Unit = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(clock.now)
    if (spawningSites.nonEmpty) {
      //trace("Found non-empty spawning site")
      spawnFish(spawningSites)
    }
  }

  private def spawnFish(sites: List[SpawningLocation]) = {
    val spawn = sites.map(site => factory.create(site, clock.now)).flatten
    //pelagicLarvaeCount += spawn.size
    pelagicLarvae ++= spawn
  }

  def canDisperse(time: DateTime): Boolean = spawn.isItSpawningSeason(time) || pelagicLarvae.nonEmpty

}

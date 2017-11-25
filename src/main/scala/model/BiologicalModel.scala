
package model

import java.io.File
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import biology._
import biology.fish._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.config.ConfigMappings._
import io.config.Configuration
import locals.{DielVerticalMigrationType, LarvaType}
import locals.Constants.LightWeightException
import locals._
import maths.{Geometry, RandomNumberGenerator}
import maths.integration.RungeKuttaIntegration
import utilities.Time
import physical.Velocity
import physical.GeoCoordinate
import physical.habitat.HabitatManager

class BiologicalModel(val config: Configuration, clock: SimulationClock, integrator: RungeKuttaIntegration) extends Logging {

  val fish: FishParameters = config.fish
  val factory = LarvaFactory.apply(LarvaType.Fish, config.fish)
  //val mortality = new MortalityDecay(config.fish.pelagicLarvalDuration.mean)
  val spawn = new Spawn(config.spawn)
  var habitatManager: HabitatManager = new HabitatManager(new File(config.inputFiles.habitatFilePath), config.habitat.buffer, Array("Reef", "Other"))
  var pelagicLarvae: ArrayBuffer[Larva] = ArrayBuffer.empty
  val stationaryLarvae: ArrayBuffer[Larva] = ArrayBuffer.empty
  val geometry = new Geometry
  val mortality = new MortalityConstant(config.fish.mortalityRate)

  def apply(): Unit = {
    spawnLarvae()
    pelagicLarvae.par.foreach(fish => biology(fish))
    refresh()
  }

  def applyMortality() : Unit = {
    pelagicLarvae.foreach(larva => mortality(larva))
    refresh()
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
    //mortality(larva)
  }

  private def move(larva: Larva): Unit = {
    val dampeningFactor: List[Double] = List(1.0)
    val orientate = swim(larva)
    def moveParticle(larva : Larva, dampeningFactor : List[Double]) : Unit = {
      if(dampeningFactor.nonEmpty) {
        integrator.integrate(larva.position, clock.now, orientate, dampeningFactor.head) match {
          case Some(newPosition) => larva.move(newPosition)
          case None => {
            moveParticle(larva, dampeningFactor.tail)
          }
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
      if(clock.isSunRising(larva.position)) {
        larva.dielVerticallyMigrate(DielVerticalMigrationType.Day)
      } else if(clock.isSunSetting(larva.position)) {
        larva.dielVerticallyMigrate(DielVerticalMigrationType.Night)
      }
    }
    if(larva.undergoesOntogeneticMigration &&
      ((larva.ontogeneticVerticallyMigrateType == StageMigration && larva.changedOntogeneticState) || (larva.ontogeneticVerticallyMigrateType == TimestepMigration)
      || larva.ontogeneticVerticallyMigrateType == DailyMigration && clock.isMidnight)) {

      larva.ontogeneticVerticallyMigrate
    }
  }

  private def ageLarvae(larva: Larva): Unit = {
    larva growOlder clock.timeStep.totalSeconds
  }

  private def mortality(larva: Larva): Unit = {
    if(RandomNumberGenerator.get < mortality.getRate) {
      larva.kill()
    }
  }

  private def sense(larva : Larva) : Unit = {
    if(larva.inOlfactoryCompetencyWindow || larva.inSettlementCompetencyWindow) {
      val index = habitatManager.getClosestHabitat(larva.position)
      if(index._1 != LightWeightException.NoReefToSettle && larva.inSettlementCompetencyWindow)  {
        larva.settle(index._1, clock.now)
      } else {
        if(index._2 != LightWeightException.NoReefSensed && larva.inOlfactoryCompetencyWindow) {
          larva.changeDirection(index._3)
        } else {
          larva.changeDirection(LightWeightException.NoSwimmingAngle)
        }
      }
    }
  }

  private def lifespanCheck(larva: Larva): Unit = {
    if (larva.isPelagic && larva.isTooOld) {
      larva.kill()
    }
  }

  private def spawnLarvae(): Unit = {
    val spawningSites = spawn.getSitesWhereFishAreSpawning(clock.now)
    if (spawningSites.nonEmpty) {
      spawnFish(spawningSites)
    }
  }

  private def spawnFish(sites: List[SpawningLocation]) = {
    val spawn = sites.flatMap(site => factory.create(site, clock.now))
    pelagicLarvae ++= spawn
  }

  def canDisperse(time: LocalDateTime): Boolean ={
    val canSpawn = spawn.isItSpawningSeason(time)
    val pelagic = pelagicLarvae.nonEmpty
    canSpawn || pelagic
  }

}

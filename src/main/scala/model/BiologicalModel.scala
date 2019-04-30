package model

import java.io.File
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import biology._
import biology.fish._
import biology.swimming._
import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.config.ConfigMappings._
import io.config.Configuration
import locals._
import locals.Constants.LightWeightException._
import maths.{Geometry, RandomNumberGenerator}
import maths.integration.RungeKuttaIntegration
import utilities.Time
import physical.Velocity
import physical.GeoCoordinate
import physical.habitat.HabitatManager

class BiologicalModel(
    val config: Configuration,
    clock: SimulationClock,
    integrator: RungeKuttaIntegration
) extends Logging {

  //val fish: FishParameters = config.fish
  val factory = LarvaeFactory.apply(Fish, config.larva)
  val spawn = new Spawn(config.spawn)
  var habitatManager: HabitatManager = new HabitatManager(
    new File(config.inputFiles.pathHabitatShapeFile),
    config.habitat.buffer,
    Array("Reef", "Other")
  )
  var pelagicLarvae: ArrayBuffer[Larva] = ArrayBuffer.empty
  val stationaryLarvae: ArrayBuffer[Larva] = ArrayBuffer.empty
  val geometry = new Geometry
  val mortality = new MortalityConstant(config.larva.mortalityRate)

  def apply(): Unit = {
    spawnLarvae()
    pelagicLarvae.par.foreach(fish => biology(fish))
    refresh()
  }

  def applyMortality(): Unit = {
    pelagicLarvae.foreach(larva => mortality(larva))
    refresh()
  }

  def refresh(): Unit = {
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
    def moveParticle(larva: Larva, dampeningFactor: List[Double]): Unit = {
      if (dampeningFactor.nonEmpty) {
        integrator.integrate(
          larva.position,
          clock.now,
          orientate,
          dampeningFactor.head
        ) match {
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

  private def swim(larva: Larva): Option[Velocity] = {
    larva.horizontalSwimming match {
      case Some(swimming) => {
        if (swimming.isDirected && larva.canSwim && larva.direction != -1) {
          Some(
            swimming(
              new HorizontalSwimmingVariables(larva.direction, 0, 0, 0)
            )
          )
        } else {
          None
        }
      }
      case None => None
    }
  }

  private def migrateLarvaVertically(larva: Larva): Unit = {
    if (larva.undergoesDielMigration) {
      if (clock.isSunRising(larva.position)) {
        //larva.diel(Day)
      } else if (clock.isSunSetting(larva.position)) {
        //larva.diel(Night)
      }
    }
    //if (larva.undergoesOntogeneticMigration &&
    //((larva.ontogeneticVerticallyMigrateType == StageMigration && larva.changedOntogeneticState) || (larva.ontogeneticVerticallyMigrateType == TimeStepMigration)
    //|| larva.ontogeneticVerticallyMigrateType == DailyMigration && clock.isMidnight)) {

    larva.ovm
    //}
  }

  private def ageLarvae(larva: Larva): Unit = {
    larva growOlder clock.timeStep.totalSeconds
  }

  private def mortality(larva: Larva): Unit = {
    if (RandomNumberGenerator.get < mortality.getRate) {
      larva.kill()
    }
  }

  private def sense(larva: Larva): Unit = {
    if (larva.inOlfactoryCompetencyWindow || larva.inSettlementCompetencyWindow) {
      val index = habitatManager.getClosestHabitat(larva.position)
      if (index._1 != NoReefToSettleException && larva.inSettlementCompetencyWindow) {
        larva.settle(index._1, clock.now)
      } else {
        if (index._2 != NoReefSensedException && larva.inOlfactoryCompetencyWindow) {
          larva.changeDirection(index._3)
        } else {
          larva.changeDirection(NoSwimmingAngleException)
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
    val spawn = sites.flatMap(
      site =>
        for (i <- 1 to site.numberOfLarvae)
          yield factory.create(site, clock.now)
    )
    pelagicLarvae ++= spawn
  }

  def canDisperse(time: LocalDateTime): Boolean = {
    val canSpawn = spawn.isItSpawningSeason(time)
    val pelagic = pelagicLarvae.nonEmpty
    canSpawn || pelagic
  }

}

package model

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.parallel.CollectionConverters._
import biology._
import exceptions.CoordinateOutOfBoundsException
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

  val factory = LarvaeFactory.apply(locals.Fish, config.larva)
  val spawn = new Spawn(config.spawn)
  var habitatManager: HabitatManager = new HabitatManager(
    config.habitat
  )
  var pelagicLarvae: ArrayBuffer[Larva] = ArrayBuffer.empty
  val stationaryLarvae: ArrayBuffer[Larva] = ArrayBuffer.empty
  private val lostFromDomain = new AtomicInteger(0)

  /** The number of larvae that drifted off the edge of the flow grid and were killed. */
  def larvaeLostFromDomain: Int = lostFromDomain.get
  val geometry = new Geometry
  val mortality: Mortality = new MortalityConstant(
    config.larva.mortalityRate.getOrElse(1)
  )

  def apply(): Unit = {
    spawnLarvae()
    debug("Spawned " + pelagicLarvae.size + " larvae")
    val step = clock.now.toDateTime(DateTimeZone.UTC).getMillis
    pelagicLarvae.par.foreach(
      fish =>
        RandomNumberGenerator.withStream(
          RandomNumberGenerator.keyFor(fish.id),
          step
        ) { biology(fish) }
    )
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

  /*
   Cycles through all the biological processes per time iteration
   - increment the age
   - kill if too old
   - move
   - sense habitat
   */
  private def biology(larva: Larva): Unit = {
    val recentlyDeveloped = ageLarvae(larva)
    mortalityDueToOldAgeCheck(larva)
    // A larva that has just died of old age, or has left the flow grid, is dead and must
    // not move or settle
    if (larva.isPelagic && move(larva, recentlyDeveloped)) sense(larva)
  }

  /** Moves the larva, returning false if it left the flow grid and was killed. */
  private def move(larva: Larva, recentlyDeveloped: Boolean): Boolean = {
    val dampeningFactor: List[Double] = List(1.0)
    val swimmingVelocity = larva.swim()
    def moveLarva(larva: Larva, dampeningFactor: List[Double]): Unit = {
      if (dampeningFactor.nonEmpty) {
        integrator.integrate(
          larva.position,
          clock.now,
          swimmingVelocity,
          dampeningFactor.head
        ) match {
          case Some(newPosition) => larva.move(newPosition)
          case None => {
            moveLarva(larva, dampeningFactor.tail)
          }
        }
      } else {
        larva.move(larva.position)
      }
    }
    val stillInDomain =
      try {
        moveLarva(larva, dampeningFactor)
        true
      } catch {
        case _: CoordinateOutOfBoundsException =>
          larva.kill()
          lostFromDomain.incrementAndGet()
          false
      }
    if (stillInDomain) migrateLarvaVertically(larva, recentlyDeveloped)
    stillInDomain
  }

  private def migrateLarvaVertically(
      larva: Larva,
      recentlyDeveloped: Boolean
  ): Unit = {
    // Diel migration only reacts to dawn and dusk, so skip the sun maths without it
    if (larva.diel.isDefined) {
      clock.sunEvent(larva.position) match {
        case Some(Sun.Rising)  => larva.dielMigrate(Day)
        case Some(Sun.Setting) => larva.dielMigrate(Night)
        case _                 =>
      }
    }
    larva.ovmMigrate(
      new OntogeneticMigrationVariables(recentlyDeveloped, clock.isFirstStepOfDay)
    )
  }

  private def canDielMigrate(larva: Larva): Unit = {
    larva.diel match {
      case Some(diel) => {
        if (clock.isSunRising(larva.position)) {
          larva.dielMigrate(Day)
        } else if (clock.isSunSetting(larva.position)) {
          larva.dielMigrate(Night)
        }
      }
      case None =>
    }
  }

  private def ageLarvae(larva: Larva): Boolean = {
    larva.incrementAge(clock.timeStep.totalSeconds)
  }

  private def mortality(larva: Larva): Unit = {
    if (RandomNumberGenerator.get < mortality.getRate) {
      larva.kill()
    }
  }

  private def sense(larva: Larva): Unit = {
    if (larva.isSettlementAge() || larva.isSensingAge()) {
      val index = habitatManager.getClosestHabitat(larva.position)
      if (index._1 != NoReefToSettleException && larva.isSettlementAge()) {
        larva.settle(index._1, clock.now)
      } else {
        if (index._2 != NoReefSensedException && larva.isSensingAge()) {
          larva.direction = index._3
        } else {
          larva.direction = RandomNumberGenerator.getAngle
        }
      }
    }
  }

  private def mortalityDueToOldAgeCheck(larva: Larva): Unit = {
    if (larva.isPelagic && larva.isTooOld) {
      larva.kill()
    }
  }

  // Larvae are released on the first step of a release day
  private def spawnLarvae(): Unit = {
    if (clock.isFirstStepOfDay) {
      val spawningSites = spawn.getSitesWhereFishAreSpawning(clock.now)
      if (spawningSites.nonEmpty) {
        spawnFish(spawningSites)
      }
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

  def isDispersing(time: LocalDateTime): Boolean = {
    val isSpawning = spawn.isItSpawningSeason(time)
    val isPelagic = pelagicLarvae.nonEmpty
    debug("Can spawn is " + isSpawning + " and pelagic is " + isPelagic)
    isSpawning || isPelagic
  }

}

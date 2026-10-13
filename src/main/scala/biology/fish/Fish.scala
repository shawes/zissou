package biology.fish

import grizzled.slf4j.Logging
import locals._
import locals.Constants.LightWeightException._
import physical.{GeoCoordinate, Velocity}
import com.github.nscala_time.time.Imports._
import biology._
import biology.swimming._
import scala.collection.mutable.ArrayBuffer
import maths.{Geometry, RandomNumberGenerator}
import utilities._

class Fish(
    val id: String,
    val pelagicLarvalDuration: Int,
    val maximumLifeSpan: Int,
    override val birthplace: Birthplace,
    val spawned: LocalDateTime,
    override val hatching: Int,
    override val preflexion: Int,
    override val flexion: Int,
    override val postflexion: Int,
    val ovmMigration: Option[OntogeneticMigration],
    val dielMigration: Option[DielMigration],
    val horizontalMigration: Option[HorizontalSwimming],
    val nonSettlementPeriod: Int
) extends Larva
    with Logging
    with Swimming
    with OntogenyFish
    with History {

  private var lastDielMigration: Option[DielVerticalMigrationType] = None
  private var nightDepth: Double = -1
  //private var hasChangedOntogeneticState: Boolean = false
  //var direction: Double = NoSwimmingAngleException
  val geometry = new Geometry()
  //override val history: ArrayBuffer[TimeCapsule] = fishHistory

  //override def state: PelagicLarvaeState = fishState

  override val birthday: LocalDateTime = spawned
  //override val nonSettlementPeriod = nonSettlementPeriod

  override def diel = dielMigration
  override def ovm = ovmMigration
  override def horizontalSwimming = horizontalMigration

  //override def settlementDate: LocalDateTime = settlementDate.get

  /*
   A fish can sense if is in a window where olfactory competency has developed and if it has the ability to swim in a directed fashion.
   */
  def isSensingAge(): Boolean =
    age <= pelagicLarvalDuration &&
      ontogeny == Postflexion &&
      (horizontalSwimming match {
        case Some(swimming) => swimming.isDirected
        case None           => false
      })

  //def isSettlementAge: Boolean = age >= nonSettlementPeriod

  def move(newPosition: GeoCoordinate): Unit = {
    if (newPosition != position) {
      //changeLarvaState(Pelagic)
      if (position.latitude != newPosition.latitude || position.longitude != newPosition.longitude) {
        saveState()
      }
      position = newPosition
    }
  }

  /*
   * Develops the larvae by ageing it the specified number of seconds
   */
  override def incrementAge(seconds: Int): Boolean = {
    age += seconds
    val newOntogeny = getOntogeneticStateForAge(age)
    if (newOntogeny == ontogeny) {
      false
    } else {
      ontogeny = newOntogeny
      true
    }
  }

  def swim(): Option[Velocity] = {
    horizontalSwimming match {
      case Some(swimming) => {
        swimming(
          new HorizontalSwimmingVariables(
            direction,
            age,
            preflexion,
            flexion,
            postflexion,
            pelagicLarvalDuration
          )
        )
      }
      case None => None
    }
  }

  def settle(reefId: Int, date: LocalDateTime): Unit = {
    settledHabitatId = reefId
    settlementDate = Some(date)
    changeLarvaState(Settled)
  }

  def kill(): Unit = {
    changeLarvaState(Dead)
  }

  def recordPosition(): Unit = saveState()

  private def changeLarvaState(newState: PelagicLarvaeState): Unit = {
    larvaState = newState
    saveState()
  }

  private def saveState(): Unit =
    history append new TimeCapsule(
      age,
      ontogeny,
      larvaState,
      settledHabitatId,
      position
    )

  // A larva with both migrations takes its depth from the ontogenetic one, except from dawn
  // to dusk, when the diel one takes it to a day depth. By day the ontogenetic depth is
  // kept as nightDepth, the depth it goes back to at dusk.
  private def isAtDayDepth: Boolean = diel.isDefined && lastDielMigration.contains(Day)

  override def ovmMigrate(variables: OntogeneticMigrationVariables): Unit = {
    ovm match {
      case Some(ovm) => {
        val migrates = ovm.implementation match {
          case TimeStepMigration         => true
          case OntogeneticStageMigration => variables.recentlyDeveloped
          case DailyMigration            => variables.isFirstStepOfDay
        }
        if (migrates) {
          if (isAtDayDepth) nightDepth = ovm(ontogeny, nightDepth)
          else depthMigration(ovm(ontogeny, position.depth))
        }
      }
      case None => ()
    }
  }

  // Dawn and dusk can each last more than one time step, so a larva migrates on the first
  // step of each and is left alone for the rest
  override def dielMigrate(time: DielVerticalMigrationType): Unit = {
    diel match {
      case Some(diel) if !lastDielMigration.contains(time) => {
        if (ovm.isDefined) {
          time match {
            case Day => {
              nightDepth = position.depth
              depthMigration(diel(Day))
            }
            case Night =>
              if (nightDepth >= 0) {
                depthMigration(nightDepth)
              }
          }
        } else {
          depthMigration(diel(time))
        }
        lastDielMigration = Some(time)
      }
      case _ =>
    }
  }

  private def depthMigration(depth: Double): Unit = {
    move(new GeoCoordinate(position.latitude, position.longitude, depth))
  }

  override def toString: String =
    "id:" + id + "," +
      "birthday:" + birthday + "," +
      "age:" + Time.convertSecondsToDays(age) + "," +
      "pld (seconds):" + pelagicLarvalDuration + "," +
      "pld (days):" + Time.convertSecondsToDays(pelagicLarvalDuration) + "," +
      "birthplace:" + birthplace.name + "," +
      "larvaState:" + larvaState + "," +
      "history:" + history.size + "," +
      "ontogeny flexion age:" + Time.convertSecondsToDays(flexion)

}

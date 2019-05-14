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
    val verticalMigrationOntogenetic: OntogeneticMigration,
    val verticalMigrationDiel: VerticalMigrationDiel,
    val nonSettlementPeriod: Int
) extends Larva
    with Logging
    with Swimming
    with OntogenyFish
    with History {

  //var fishPolygon: Int = 0
  private var lastDielMigration: Option[DielVerticalMigrationType] = None
  private var nightDepth: Double = -1
  //private var hasChangedOntogeneticState: Boolean = false
  //var direction: Double = NoSwimmingAngleException
  val geometry = new Geometry()
  //override val history: ArrayBuffer[TimeCapsule] = fishHistory

  //override def state: PelagicLarvaeState = fishState

  override val birthday: LocalDateTime = spawned

  //override def settlementDate: LocalDateTime = settlementDate.get

  /*
   A fish can sense if is in a window where olfactory competency has developed and if it has the ability to swim in a directed fashion.
   */
  def canSense: Boolean =
    age <= pelagicLarvalDuration &&
      ontogeny == Postflexion &&
      (horizontalSwimming match {
        case Some(swimming) => swimming.isDirected
        case None           => false
      })

  def canSettle: Boolean = age >= nonSettlementPeriod

  def canSwim: Boolean =
    ontogeny == Flexion || ontogeny == Postflexion

  def move(newPosition: GeoCoordinate): Unit = {
    if (newPosition != position) {
      changeLarvaState(Pelagic)
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
        if (swimming.isDirected && canSwim && direction != NoSwimmingAngleException) {
          Some(
            swimming(
              new HorizontalSwimmingVariables(
                direction,
                age,
                preflexion,
                pelagicLarvalDuration
              )
            )
          )
        } else {
          None
        }
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

  override def ovmMigrate(variables: OntogeneticMigrationVariables): Unit = {
    ovm match {
      case Some(ovm) => {
        ovm.implementation match {
          case TimeStepMigration =>
            depthMigration(ovm(ontogeny, position.depth))
          case OntogeneticStageMigration =>
            if (variables.recentlyDeveloped)
              depthMigration(ovm(ontogeny, position.depth))
          case DailyMigration =>
            if (variables.isMidnight)
              depthMigration(ovm(ontogeny, position.depth))
          case _ =>
        }
      }
      case None => ()
    }
  }

  override def dielMigrate(time: DielVerticalMigrationType): Unit = {
    diel match {
      case Some(diel) => {
        if (!lastDielMigration.isDefined || lastDielMigration.get != time) {
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
            lastDielMigration = Some(time)
          }
        }
      }
      case None =>
    }
  }

  private def depthMigration(depth: Double): Unit = {
    move(new GeoCoordinate(position.latitude, position.longitude, depth))
  }

  override def toString: String =
    "id:" + id + "," +
      "birthday:" + birthday + "," +
      "age:" + age / Constants.SecondsInDay + "," +
      "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
      "birthplace:" + birthplace.name + "," +
      "larvaState:" + larvaState + "," +
      "history:" + history.size

}

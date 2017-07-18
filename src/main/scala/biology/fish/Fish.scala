package biology.fish

import grizzled.slf4j.Logging
import locals.OntogenyState.OntogenyState
import locals.SwimmingAbility.SwimmingAbility
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.DielVerticalMigrationType.DielVerticalMigrationType
import locals.{Constants, OntogenyState, PelagicLarvaeState, SwimmingAbility}
import org.joda.time.DateTime
import physical.GeoCoordinate
import physical.Velocity
import physical.habitat.HabitatPolygon
import com.github.nscala_time.time.Imports._
import maths.RandomNumberGenerator
import biology._

import scala.collection.mutable.ListBuffer

class Fish(
  val id: Int,
  val pelagicLarvalDuration: Int,
  val maximumLifeSpan: Int,
  val birthplace: Birthplace,
  val spawned: DateTime,
  val fishOntogeny: FishOntogeny,
  val swimming: Swimming,
  val verticalMigrationOntogenetic: FishVerticalMigrationOntogenetic,
  val verticalMigrationDiel: VerticalMigrationDiel)
  extends Larva with Logging {

  val fishHistory = ListBuffer.empty[TimeCapsule]
  var fishState = PelagicLarvaeState.Pelagic
  var fishAge = 0
  var fishSettlementDate: Option[DateTime] = None
  var fishPosition = birthplace.location
  var fishPolygon: Option[HabitatPolygon] = None //TODO: Think about how this works

  def this() = this(0, 0, 0, null, DateTime.now(), null, null, null, null)

  override def settlementDate: DateTime = fishSettlementDate.get

  def inCompetencyWindow: Boolean = age < pelagicLarvalDuration && getOntogeny == OntogenyState.Postflexion //TODO: Need to code in a better competency window

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  override def ontogeny: Ontogeny = fishOntogeny

  override def age: Int = fishAge

  def undergoesOntogeneticMigration : Boolean = verticalMigrationOntogenetic.probabilities.nonEmpty

  def undergoesDielMigration : Boolean = verticalMigrationDiel.probabilities.nonEmpty

  def move(newPosition: GeoCoordinate): Unit = {
    if (newPosition.isValid) {
      changeState(PelagicLarvaeState.Pelagic)
      updatePosition(newPosition)
      //updateHabitat(newHabitat)
    } else {
      error("The position the larva is being asked to move to is not valid")
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = fishPosition = newPos

  //def horizontalSwimmingSpeed: Double = 0.0 //TODO: Implement the swimming speed

  def growOlder(seconds: Int): Unit = {
    //val initialOntogeny = getOntogeny
    fishAge += seconds
    //val currentOntogeny = getOntogeny
  }

  def settle(settlementReef: HabitatPolygon, date: DateTime): Unit = {
    updateHabitat(settlementReef)
    fishSettlementDate = Some(date)
    changeState(PelagicLarvaeState.Settled)
  }

  def swim() : Velocity = {
    //val angle = orientate
    null
  }

  private def orientate(): Double = {
    if(swimming.isDirected) {
      0
    } else
    return 1
      //TODO: Needs to find the nearest fishPolygon
      //TODO: From there is the distance is below a scent threshold, swim towards it
  }

  def updateHabitat(newHabitat: HabitatPolygon): Unit = fishPolygon = Some(newHabitat)

  def kill(): Unit = {
    changeState(PelagicLarvaeState.Dead)
  }

  private def changeState(newState: PelagicLarvaeState): Unit = {
    fishState = newState
    saveState()
  }

  private def saveState() = history append new TimeCapsule(age, getOntogeny, state, polygon.orNull, position)

  override def position: GeoCoordinate = fishPosition

  override def polygon: Option[HabitatPolygon] = fishPolygon

  override def ontogeneticVerticallyMigrate: Unit = {
    val depth = verticalMigrationOntogenetic.getDepth(getOntogeny)
    val newDepth = new GeoCoordinate(position.latitude, position.longitude, depth)
    updatePosition(newDepth)
  }

  override def dielVerticallyMigrate(dielMigration : DielVerticalMigrationType) : Unit = {
    val depth = verticalMigrationDiel.getDepth(dielMigration)
    val newDepth = new GeoCoordinate(position.latitude, position.longitude, depth)
    updatePosition(newDepth)
  }

  override def toString: String =
    "id:" + id + "," +
    "birthday:" + birthday + "," +
    "age:" + age / Constants.SecondsInDay + "," +
    "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
    "birthplace:" + birthplace.name + "," +
    "state:" + state + "," +
    "history:" + history.size

  override def history: ListBuffer[TimeCapsule] = fishHistory

  override def state: PelagicLarvaeState = fishState

  override def birthday: DateTime = spawned




}

package biology.fish

import grizzled.slf4j.Logging
import locals.OntogenyState.OntogenyState
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.DielVerticalMigrationType.DielVerticalMigrationType
import locals.{Constants, OntogenyState, PelagicLarvaeState}
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import biology._
import scala.collection.mutable.ArrayBuffer

class Fish(
  val id: Int,
  val pelagicLarvalDuration: Int,
  val maximumLifeSpan: Int,
  val birthplace: Birthplace,
  val spawned: DateTime,
  val fishOntogeny: FishOntogeny,
  val swimming: Swimming,
  val verticalMigrationOntogenetic: FishVerticalMigrationOntogenetic,
  val verticalMigrationDiel: VerticalMigrationDiel,
  val nonSettlementPeriod: Int)
  extends Larva with Logging {

  val fishHistory = ArrayBuffer.empty[TimeCapsule]
  var fishState = PelagicLarvaeState.Pelagic
  var fishAge = 0
  var fishSettlementDate: Option[DateTime] = None
  var fishPosition = birthplace.location
  var fishPolygon: Int = 0
  var lastDielMigration : Option[DielVerticalMigrationType] = None
  private var hasChangedOntogeneticState : Boolean = false
  var fishDirection : Double = -1

  def this() = this(0, 0, 0, null, DateTime.now(), null, null, null, null, 0)

  override def settlementDate: DateTime = fishSettlementDate.get

  override def changedOntogeneticState : Boolean = hasChangedOntogeneticState

  override def direction : Double = fishDirection

  override def changeDirection(angle : Double) = fishDirection = angle

  def inCompetencyWindow: Boolean = age <= pelagicLarvalDuration && getOntogeny == OntogenyState.Postflexion && age > nonSettlementPeriod

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  override def ontogeny: FishOntogeny = fishOntogeny

  override def age: Int = fishAge

  def undergoesOntogeneticMigration : Boolean =  {
    trace("Checking OVM, size is "+ verticalMigrationOntogenetic.probabilities.size)
    verticalMigrationOntogenetic.probabilities.nonEmpty
  }

  def undergoesDielMigration : Boolean = verticalMigrationDiel.probabilities.nonEmpty

  def move(location: GeoCoordinate): Unit = {
    if(location != fishPosition) {
      changeState(PelagicLarvaeState.Pelagic)
      updatePosition(location)
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = {
    fishPosition = newPos
  }

  def growOlder(seconds: Int): Unit = {
    val initialOntogeny = getOntogeny
    fishAge += seconds
    val currentOntogeny = getOntogeny
    if(initialOntogeny == currentOntogeny) {
      hasChangedOntogeneticState = false
    } else {
      hasChangedOntogeneticState = true
    }
  }

  def settle(reefId : Int, date: DateTime): Unit = {
    updateHabitat(reefId)
    fishSettlementDate = Some(date)
    changeState(PelagicLarvaeState.Settled)
  }

  def updateHabitat(reefId: Int): Unit = {
    fishPolygon = reefId
  }

  def kill(): Unit = {
    changeState(PelagicLarvaeState.Dead)
  }

  private def changeState(newState: PelagicLarvaeState): Unit = {
    fishState = newState
    saveState()
  }

  private def saveState() : Unit =
    fishHistory append new TimeCapsule(age, getOntogeny, state, polygon, position)

  override def position: GeoCoordinate = fishPosition

  override def polygon: Int = fishPolygon

  override def ontogeneticVerticallyMigrate: Unit = {
    val depth = verticalMigrationOntogenetic.getDepth(getOntogeny)
    updatePosition(new GeoCoordinate(position.latitude, position.longitude, depth))
  }

  override def dielVerticallyMigrate(dielMigration : DielVerticalMigrationType) : Unit = {
    if(!lastDielMigration.isDefined || lastDielMigration.get != dielMigration) {
      val depth = verticalMigrationDiel.getDepth(dielMigration)
      val newDepth = new GeoCoordinate(position.latitude, position.longitude, depth)
      updatePosition(newDepth)
      lastDielMigration = Some(dielMigration)
   }
  }

  override def toString: String =
    "id:" + id + "," +
    "birthday:" + birthday + "," +
    "age:" + age / Constants.SecondsInDay + "," +
    "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
    "birthplace:" + birthplace.name + "," +
    "state:" + state + "," +
    "history:" + history.size

  override def history: ArrayBuffer[TimeCapsule] = fishHistory

  override def state: PelagicLarvaeState = fishState

  override def birthday: DateTime = spawned

}

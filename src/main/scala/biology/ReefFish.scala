package biology

import grizzled.slf4j.Logging
import locals.OntogenyState.OntogenyState
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.{Constants, OntogenyState, PelagicLarvaeState}
import org.joda.time.DateTime
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon
import utilities.SimpleCounter

import scala.collection.mutable.ListBuffer

class ReefFish(val id: Int,
               val pelagicLarvalDuration: Int,
               val maximumLifeSpan: Int,
               val birthplace: Birthplace,
               val spawned: DateTime,
               val reefFishOntogeny: ReefFishOntogeny,
               val verticalMigration: VerticalMigration)
  extends Larva with Logging {

  val reefFishState = new State(PelagicLarvaeState.Pelagic)
  val reefFishAge = new SimpleCounter(0)
  val reefFishHistory = ListBuffer.empty[TimeCapsule]
  var reefFishSettlementDate: Option[DateTime] = None
  var reefFishPosition = birthplace.location
  var reefFishPolygon: Option[HabitatPolygon] = None //TODO: Think about how this works

  def this() = this(0, 0, 0, null, DateTime.now(), null, null)

  override def settlementDate: DateTime = reefFishSettlementDate.get

  def inCompetencyWindow: Boolean = age < pelagicLarvalDuration && getOntogeny == OntogenyState.Postflexion //TODO: Need to code in a better competency window

  def move(newPosition: GeoCoordinate): Unit = {
    if (newPosition.isValid) {
      changeState(PelagicLarvaeState.Pelagic)
      updatePosition(newPosition)
      //updateHabitat(newHabitat)
    } else {
      error("The position the larva is being asked to move to is not valid")
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = reefFishPosition = newPos

  private def changeState(newState: PelagicLarvaeState): Unit = {
    reefFishState.change(newState)
    saveState()
  }

  private def saveState() = history append new TimeCapsule(age, getOntogeny, state, polygon, position)

  override def history: ListBuffer[TimeCapsule] = reefFishHistory

  override def position: GeoCoordinate = reefFishPosition

  override def polygon: HabitatPolygon = reefFishPolygon.get

  override def state: PelagicLarvaeState = reefFishState.state

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  override def age: Int = reefFishAge.count

  override def ontogeny: Ontogeny = reefFishOntogeny

  def horizontalSwimmingSpeed: Double = 0.0 //TODO: Implement the swimming speed

  def growOlder(seconds: Int): Unit = reefFishAge.increment(seconds)

  def settle(settlementReef: HabitatPolygon, date: DateTime): Unit = {
    updateHabitat(settlementReef)
    reefFishSettlementDate = Some(date)
    changeState(PelagicLarvaeState.Settled)
  }

  def updateHabitat(newHabitat: HabitatPolygon): Unit = reefFishPolygon = Some(newHabitat)

  def kill(): Unit = {
    changeState(PelagicLarvaeState.Dead)
  }

  override def getOntogeneticVerticalMigrationDepth: Double = {
    verticalMigration.getDepth(getOntogeny)
  }

  override def toString: String =
    "id:" + id + "," +
      "birthday:" + birthday + "," +
      "age:" + age / Constants.SecondsInDay + ","

  override def birthday: DateTime = spawned

  "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
    "birthplace:" + birthplace.name + "," +
    "state:" + state + "," +
    "history:" + history.size



}

case class State(state: PelagicLarvaeState) {
  def change(newState: PelagicLarvaeState): Unit = copy(state = newState)
}

case class Date(date: DateTime) {
  def change(newDate: DateTime): Unit = copy(date = newDate)
}

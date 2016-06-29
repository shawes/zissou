package biology

import grizzled.slf4j.Logging
import locals.OntogenyState.OntogenyState
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.{Constants, HabitatType, OntogenyState, PelagicLarvaeState}
import org.joda.time.DateTime
import physical.GeoCoordinate
import physical.habitat.{GeometryAdaptor, HabitatPolygon}

import scala.collection.mutable.ListBuffer

class ReefFish(val id: Int,
               val pelagicLarvalDuration: Int,
               val maximumLifeSpan: Int,
               val birthplace: Birthplace,
               val spawned: DateTime,
               val reefFishOntogeny: ReefFishOntogeny,
               val verticalMigration: VerticalMigration)
  extends Larva with Logging {

  var reefFishState = PelagicLarvaeState.Pelagic
  var reefFishAge = 0
  var reefFishHistory = ListBuffer.empty[TimeCapsule]
  var reefFishSettlementDate = Constants.MinimumDate
  var reefFishPosition = birthplace.location
  var reefFishPolygon: HabitatPolygon = new GeometryAdaptor(null, -1, HabitatType.Ocean) //TODO: Think about how this works

  def this() = this(0, 0, 0, null, DateTime.now(), null, null)

  override def settlementDate: DateTime = reefFishSettlementDate

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
    reefFishState = newState
    saveState()
  }

  private def saveState() = history append new TimeCapsule(age, getOntogeny, state, polygon, position)

  override def history: ListBuffer[TimeCapsule] = reefFishHistory

  override def position: GeoCoordinate = reefFishPosition

  override def polygon: HabitatPolygon = reefFishPolygon

  override def state: PelagicLarvaeState = reefFishState

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  override def age: Int = reefFishAge

  override def ontogeny: Ontogeny = reefFishOntogeny

  def growOlder(seconds: Int): Unit = reefFishAge += seconds

  def settle(settlementReef: HabitatPolygon, date: DateTime): Unit = {
    updateHabitat(settlementReef)
    reefFishSettlementDate = date
    changeState(PelagicLarvaeState.Settled)
  }

  def updateHabitat(newHabitat: HabitatPolygon): Unit = reefFishPolygon = newHabitat

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

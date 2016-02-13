package biology


import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import locals.OntogenyState._
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.{Constants, PelagicLarvaeState}
import maths.RandomNumberGenerator
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

import scala.collection.mutable.ListBuffer


abstract class Larva(val id: Int,
                     val pelagicLarvalDuration: Int,
                     val maximumLifeSpan: Int,
                     val birthplace: Birthplace,
                     val birthday: DateTime,
                     val ontogeny: Ontogeny,
                     val verticalMigration: VerticalMigration) extends Logging {

  val history: ListBuffer[TimeCapsule] = ListBuffer.empty[TimeCapsule]
  var state = PelagicLarvaeState.Pelagic
  var age: Int = 0

  var settlementDate : DateTime = Constants.MinimumDate
  var position: GeoCoordinate = birthplace.location
  var polygon: HabitatPolygon
  var hasSettled: Boolean = false


  def hasBeenPelagicTooLong: Boolean = age >= pelagicLarvalDuration

  def isTooOld: Boolean = age >= maximumLifeSpan

  def inCompetencyWindow: Boolean = age < pelagicLarvalDuration //TODO: Need to code in the competency window

  def isPelagic: Boolean = state == PelagicLarvaeState.Pelagic

  def isDead: Boolean = state == PelagicLarvaeState.Dead

  def isSettled: Boolean = state == PelagicLarvaeState.Settled

  def move(newPosition: GeoCoordinate, newHabitat: HabitatPolygon): Unit = {
    if (newPosition.isValid) {
      updatePosition(newPosition)
      updateHabitat(newHabitat)
      changeState(PelagicLarvaeState.Pelagic)
    } else {
      error("The position the larva is being asked to move to is not valid")
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = position = newPos

  def growOlder(seconds: Int): Unit = age += seconds

  def settle(settlementReef: HabitatPolygon, date: DateTime) : Unit = {
    updateHabitat(settlementReef)
    settlementDate = date
    state = PelagicLarvaeState.Settled
    saveState()
  }

  def updateHabitat(newHabitat: HabitatPolygon): Unit = polygon = newHabitat

  private def saveState(): Unit = {
    debug("Save state called")
    val currentState = new TimeCapsule(age, getOntogeny, state, polygon, position)
    history append currentState
    debug("History is has this saved " + history.size)
  }

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  def kill(): Unit = {
    changeState(PelagicLarvaeState.Dead)

  }

  private def changeState(newState: PelagicLarvaeState): Unit = {
    state = newState
    saveState()
  }

  def getOntogeneticVerticalMigrationDepth(random: RandomNumberGenerator): Double = {
    verticalMigration.getDepth(getOntogeny, random)
  }

  override def toString: String = "id:" + id + "," +
    "birthday:" + birthday + "," +
    "age:" + age / Constants.SecondsInDay + ","

  "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
    "birthplace:" + birthplace.name + "," +
    "state:" + state + "," +
    "history:" + history.size
}

package biology


import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import locals.OntogenyState._
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.{Constants, OntogenyState, PelagicLarvaeState}
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
  var age: Int = 0
  var settlementDate : DateTime = Constants.MinimumDate
  var position: GeoCoordinate = birthplace.location
  var polygon: HabitatPolygon
  private var state = PelagicLarvaeState.Pelagic


  //def hasBeenPelagicTooLong: Boolean = age >= pelagicLarvalDuration

  def isTooOld: Boolean = age > maximumLifeSpan

  def inCompetencyWindow: Boolean = age < pelagicLarvalDuration && getOntogeny == OntogenyState.Postflexion //TODO: Need to code in the competency window

  def isPelagic: Boolean = state == PelagicLarvaeState.Pelagic

  def isDead: Boolean = state == PelagicLarvaeState.Dead

  def isSettled: Boolean = state == PelagicLarvaeState.Settled

  def move(newPosition: GeoCoordinate): Unit = {
    if (newPosition.isValid) {
      changeState(PelagicLarvaeState.Pelagic)
      updatePosition(newPosition)
      //updateHabitat(newHabitat)
    } else {
      error("The position the larva is being asked to move to is not valid")
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = position = newPos

  def growOlder(seconds: Int): Unit = age += seconds

  def settle(settlementReef: HabitatPolygon, date: DateTime): Unit = {
    //saveState()
    updateHabitat(settlementReef)
    settlementDate = date
    changeState(PelagicLarvaeState.Settled)

  }

  private def changeState(newState: PelagicLarvaeState): Unit = {
    state = newState
    saveState()
  }

  private def saveState() = history append new TimeCapsule(age, getOntogeny, state, polygon, position)

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  def updateHabitat(newHabitat: HabitatPolygon): Unit = polygon = newHabitat

  def kill(): Unit = {
    changeState(PelagicLarvaeState.Dead)
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

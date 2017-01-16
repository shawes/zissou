package biology


import com.github.nscala_time.time.Imports._
import locals.OntogenyState._
import locals.PelagicLarvaeState
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

import scala.collection.mutable.ListBuffer


trait Larva {

  def id: Int

  def birthday: DateTime

  def age: Int

  def maximumLifeSpan: Int

  def birthplace: Birthplace

  def settlementDate: DateTime

  def polygon: Option[physical.habitat.HabitatPolygon]

  def history: ListBuffer[TimeCapsule]

  def position: GeoCoordinate

  def state: PelagicLarvaeState

  def ontogeny: Ontogeny

  def horizontalSwimmingSpeed: Double

  def isTooOld: Boolean = age > maximumLifeSpan
  def isPelagic: Boolean = state == PelagicLarvaeState.Pelagic
  def isDead: Boolean = state == PelagicLarvaeState.Dead
  def isSettled: Boolean = state == PelagicLarvaeState.Settled

  def move(newPosition: GeoCoordinate)

  def updatePosition(newPos: GeoCoordinate)

  def growOlder(seconds: Int)

  def settle(settlementReef: HabitatPolygon, date: DateTime)

  def getOntogeny: OntogenyState

  def updateHabitat(newHabitat: HabitatPolygon)

  def kill()

  def getOntogeneticVerticalMigrationDepth: Double

  def getDielVerticalMigrationDepth(time : DateTime) : Double

  def inCompetencyWindow: Boolean

}

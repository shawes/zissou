package biology


import scala.collection.mutable.ListBuffer

import com.github.nscala_time.time.Imports._
import locals.OntogenyState._
import locals.SwimmingAbility._
import locals.PelagicLarvaeState
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.DielVerticalMigrationType.DielVerticalMigrationType
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon


trait Larva {

  def id: Int

  def birthday: DateTime

  def age: Int

  def maximumLifeSpan: Int

  def birthplace: Birthplace

  def pelagicLarvalDuration: Int

  def settlementDate: DateTime

  def polygon: Option[physical.habitat.HabitatPolygon]

  def history: ListBuffer[TimeCapsule]

  def position: GeoCoordinate

  def state: PelagicLarvaeState

  def ontogeny: Ontogeny

  def swimming : Swimming

  def move(newPosition: GeoCoordinate) : Unit

  def updatePosition(newPos: GeoCoordinate) : Unit

  def growOlder(seconds: Int) : Unit

  def settle(settlementReef: HabitatPolygon, date: DateTime) : Unit

  def getOntogeny: OntogenyState

  def updateHabitat(newHabitat: HabitatPolygon) : Unit

  def kill() : Unit

  def ontogeneticVerticallyMigrate(): Unit

  def dielVerticallyMigrate(dielMigration : DielVerticalMigrationType) : Unit

  def inCompetencyWindow: Boolean

  def isTooOld: Boolean = age > maximumLifeSpan

  def isPelagic: Boolean = state == PelagicLarvaeState.Pelagic

  def isDead: Boolean = state == PelagicLarvaeState.Dead

  def isSettled: Boolean = state == PelagicLarvaeState.Settled

  def undergoesDielMigration : Boolean

  def undergoesOntogeneticMigration : Boolean

  def changedOntogeneticState : Boolean

}

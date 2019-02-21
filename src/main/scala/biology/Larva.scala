package biology

import scala.collection.mutable.ArrayBuffer

import com.github.nscala_time.time.Imports._
import locals.DielVerticalMigrationType.DielVerticalMigrationType
import locals.OntogeneticVerticalMigrationImpl
import locals.OntogenyState._
import locals.PelagicLarvaeState
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

trait Larva {

  def id: Int
  def birthday: LocalDateTime
  def age: Int
  def maximumLifeSpan: Int
  def birthplace: Birthplace
  def pelagicLarvalDuration: Int
  def settlementDate: LocalDateTime
  def polygon: Int
  def history: ArrayBuffer[TimeCapsule]
  def position: GeoCoordinate
  def state: PelagicLarvaeState
  def ontogeny: Ontogeny
  def swimming : Swimming
  def direction : Double
  def move(newPosition: GeoCoordinate) : Unit
  def updatePosition(newPos: GeoCoordinate) : Unit
  def growOlder(seconds: Int) : Unit
  def settle(reefId : Int, date: LocalDateTime) : Unit
  def getOntogeny: OntogenyState
  def updateHabitat(reefId : Int) : Unit
  def kill() : Unit
  def ontogeneticVerticallyMigrate(): Unit
  def ontogeneticVerticallyMigrateType : OntogeneticVerticalMigrationImpl
  def dielVerticallyMigrate(dielMigration : DielVerticalMigrationType) : Unit
  def inOlfactoryCompetencyWindow: Boolean
  def inSettlementCompetencyWindow : Boolean
  def isTooOld : Boolean = age > maximumLifeSpan
  def isPelagic : Boolean = state == PelagicLarvaeState.Pelagic
  def isDead : Boolean = state == PelagicLarvaeState.Dead
  def isSettled : Boolean = state == PelagicLarvaeState.Settled
  def undergoesDielMigration : Boolean
  def undergoesOntogeneticMigration : Boolean
  def changedOntogeneticState : Boolean
  def changeDirection(angle : Double) : Unit
  def canSwim() : Boolean
}

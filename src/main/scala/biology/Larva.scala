package biology

import scala.collection.mutable.ArrayBuffer

import com.github.nscala_time.time.Imports._
import locals._
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon
import biology.swimming._

trait Larva extends Swimming {

  val id: String
  val history: ArrayBuffer[TimeCapsule]
  val birthday: LocalDateTime
  var age: Int = 0
  val maximumLifeSpan: Int
  val birthplace: Birthplace
  val pelagicLarvalDuration: Int
  def settlementDate: LocalDateTime
  def polygon: Int
  var position: GeoCoordinate
  var state: PelagicLarvaeState = Pelagic
  def direction: Double
  def move(newPosition: GeoCoordinate): Unit
  def updatePosition(newPos: GeoCoordinate): Unit
  def growOlder(seconds: Int): Unit
  def settle(reefId: Int, date: LocalDateTime): Unit
  def getOntogeny: OntogeneticState
  def updateHabitat(reefId: Int): Unit
  def kill(): Unit
  def inOlfactoryCompetencyWindow: Boolean
  def inSettlementCompetencyWindow: Boolean
  def isTooOld: Boolean = age > maximumLifeSpan
  def isPelagic: Boolean = state == Pelagic
  def isDead: Boolean = state == Dead
  def isSettled: Boolean = state == Settled
  def undergoesDielMigration: Boolean
  def undergoesOntogeneticMigration: Boolean
  def changedOntogeneticState: Boolean
  def changeDirection(angle: Double): Unit
  def canSwim(): Boolean
}

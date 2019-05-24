package biology

import scala.collection.mutable.ArrayBuffer

import com.github.nscala_time.time.Imports._
import locals._
import physical.{GeoCoordinate, Velocity}
import physical.habitat.HabitatPolygon
import biology.swimming._

trait Larva extends Swimming with History with Ontogeny {

  val id: String
  val birthday: LocalDateTime
  var age: Int = 0
  var direction: Double =
    Constants.LightWeightException.NoSwimmingAngleException
  var larvaState: PelagicLarvaeState = Pelagic
  val maximumLifeSpan: Int
  val birthplace: Birthplace
  val pelagicLarvalDuration: Int
  var settlementDate: Option[LocalDateTime] = None
  var settledHabitatId: Int = 0
  var position: GeoCoordinate = birthplace.location
  val nonSettlementPeriod: Int

  def move(newPosition: GeoCoordinate): Unit
  def incrementAge(seconds: Int): Boolean
  def settle(reefId: Int, date: LocalDateTime): Unit
  def swim(): Option[Velocity]
  def kill()

  def dielMigrate(time: DielVerticalMigrationType): Unit
  def ovmMigrate(variables: OntogeneticMigrationVariables): Unit
  //def changedOntogeneticState: Boolean

  def isSensingAge(): Boolean
  def isSettlementAge(): Boolean = age > nonSettlementPeriod

  def isTooOld: Boolean = age > maximumLifeSpan
  def isPelagic: Boolean = larvaState == Pelagic
  def isDead: Boolean = larvaState == Dead
  def isSettled: Boolean = larvaState == Settled
}

package biology


import com.github.nscala_time.time.Imports._
import locals.{Constants, PelagicLarvaeState}
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

import scala.collection.mutable.ListBuffer


abstract class Larva(val id: Int,
                     val pelagicLarvalDuration: Int,
                     val maximumLifeSpan: Int,
                     val birthplace: Birthplace,
                     var state: PelagicLarvaeState) {
  val birthday = DateTime.now

  def history: ListBuffer[TimeCapsule] = ListBuffer.empty

  def age: Int

  var settlementDate : DateTime = Constants.MinimumDate
  var position: GeoCoordinate = birthplace.location
  var polygon: HabitatPolygon
  var hasSettled: Boolean = false

  def attainedPld : Boolean = age >= pelagicLarvalDuration

  def attainedMaximumLifeSpan : Boolean = age >= maximumLifeSpan

  def canMove : Boolean = state == PelagicLarvaeState.Pelagic

  def move(newPosition: GeoCoordinate) : Unit

  def settle(settlementReef: HabitatPolygon, date: DateTime) : Unit = {
    polygon = settlementReef
    settlementDate = date
    state = PelagicLarvaeState.Settled
  }

  def kill() : Unit = state = PelagicLarvaeState.Dead

  def saveHistory() : Unit = history += new TimeCapsule(age, state, polygon, position)


}

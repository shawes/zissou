package biology


import com.github.nscala_time.time.Imports._
import locals.PelagicLarvaeState
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

  //val id: Int
  def age: Int

  var settlementDate: DateTime = null
  //var state: PelagicLarvaeState
  var position: GeoCoordinate = birthplace.location
  var polygon: HabitatPolygon
  var hasSettled: Boolean = false

  def attainedPld = age >= pelagicLarvalDuration

  def attainedMaximumLifeSpan = age >= maximumLifeSpan

  def canMove = state == PelagicLarvaeState.Pelagic

  def move(newPosition: GeoCoordinate)

  def settle(settlementReef: HabitatPolygon, date: DateTime) {
    polygon = settlementReef
    settlementDate = date
    state = PelagicLarvaeState.Settled
  }

  def kill() = state = PelagicLarvaeState.Dead

  def saveHistory() = history += new TimeCapsule(age, state, polygon, position)


}

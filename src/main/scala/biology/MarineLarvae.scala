package biology


import physical.GeoCoordinate
import physical.habitat.HabitatPolygon
import locals.PelagicLarvaeState
import locals.PelagicLarvaeState.PelagicLarvaeState
import com.github.nscala_time.time.Imports._


abstract class MarineLarvae {
  val id: Int
  var age: Int
  val birthday: DateTime
  var settlementDate: DateTime
  var state: PelagicLarvaeState
  val pelagicLarvalDuration: Int
  val maximumLifeSpan: Int
  var currentPosition: GeoCoordinate
  var currentPolygon: HabitatPolygon
  var hasSettled: Boolean
  val history: Vector[TimeCapsule]

  def attainedPld = age >= pelagicLarvalDuration

  def attainedMaximumLifeSpan = age >= maximumLifeSpan

  def canMove = state == PelagicLarvaeState.Pelagic

  def move(newPosition: GeoCoordinate)

  def settle(polygon: HabitatPolygon, date: DateTime) {
    currentPolygon = polygon
    settlementDate = date
    state = PelagicLarvaeState.Settled
  }

  def kill() = state = PelagicLarvaeState.Dead

  def saveHistory() = history :+ new TimeCapsule(age, state, currentPolygon, currentPosition)


}

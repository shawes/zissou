package biology


import com.github.nscala_time.time.Imports._
import locals.PelagicLarvaeState
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon


abstract class MarineLarvae(val id: Int, val pelagicLarvalDuration: Int, val maximumLifeSpan: Int) {
  val birthday = DateTime.now

  def history: Vector[TimeCapsule]
  //val id: Int
  def age: Int
  var settlementDate: DateTime = null
  var state: PelagicLarvaeState
  var currentPosition: GeoCoordinate = new GeoCoordinate()
  var currentPolygon: HabitatPolygon
  var hasSettled: Boolean = false

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

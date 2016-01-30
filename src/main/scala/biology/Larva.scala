package biology


import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.{Constants, PelagicLarvaeState}
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

import scala.collection.mutable.ListBuffer


abstract class Larva(val id: Int,
                     val pelagicLarvalDuration: Int,
                     val maximumLifeSpan: Int,
                     val birthplace: Birthplace,
                     var state: PelagicLarvaeState,
                     val birthday: DateTime) extends Logging {

  //val birthday : DateTime = DateTime.now()

  val history: ListBuffer[TimeCapsule] = ListBuffer.empty[TimeCapsule]

  var age: Int = 0

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

  def saveState(): Unit = {
    debug("Save state called")
    val currentState = new TimeCapsule(age, state, polygon, position)
    history append currentState
    debug("History is has this saved " + history.size)
  }

  override def toString: String = "id:" + id + "," +
    "birthday:" + birthday + "," +
    "age:" + age / Constants.SecondsInDay + ","

  "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
    "birthplace:" + birthplace.name + "," +
    "state:" + state + "," +
    "history:" + history.size
}

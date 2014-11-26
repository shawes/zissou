package biology

import locals.{HabitatType, PelagicLarvaeState}
import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate
import physical.habitat.{GeometryAdaptor, HabitatPolygon}

class ReefFish(val id: Int,
               val pelagicLarvalDuration: Int,
               val maximumLifeSpan: Int) extends MarineLarvae {
  def this() = this(0, 0, 0)

  val birthday = DateTime.now
  var age = 0
  var state = PelagicLarvaeState.Pelagic
  var currentPosition = new GeoCoordinate()
  var hasSettled = false
  var settlementDate: DateTime = null
  var currentPolygon: HabitatPolygon = new GeometryAdaptor(null, 1, HabitatType.Reef)
  val history: Vector[TimeCapsule] = Vector.empty[TimeCapsule]

  def move(newPosition: GeoCoordinate) = {
    require(!newPosition.isUndefined)
    saveHistory()
    currentPosition = newPosition
  }

  //lazy val maximumLifeSpan: Int = 0


}

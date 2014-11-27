package biology

import locals.{HabitatType, PelagicLarvaeState}
import physical.GeoCoordinate
import physical.habitat.{GeometryAdaptor, HabitatPolygon}

class ReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int)
  extends MarineLarvae(id, pelagicLarvalDuration, maximumLifeSpan) {
  val history = Vector.empty[TimeCapsule]
  var state = PelagicLarvaeState.Pelagic
  var age = 0
  var currentPolygon: HabitatPolygon = new GeometryAdaptor(null, 1, HabitatType.Reef)

  def this() = this(0, 0, 0)

  def move(newPosition: GeoCoordinate) = {
    require(!newPosition.isUndefined)
    saveHistory()
    currentPosition = newPosition
  }

  //lazy val maximumLifeSpan: Int = 0


}

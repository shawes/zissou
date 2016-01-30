package biology

import grizzled.slf4j.Logging
import locals.HabitatType
import locals.PelagicLarvaeState.PelagicLarvaeState
import org.joda.time.DateTime
import physical.GeoCoordinate
import physical.habitat.{GeometryAdaptor, HabitatPolygon}

class ReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int, birthplace: Birthplace, state: PelagicLarvaeState, spawned: DateTime)
  extends Larva(id, pelagicLarvalDuration, maximumLifeSpan, birthplace, state, spawned) with Logging {
  //val history = Vector.empty[TimeCapsule]
  //var state = PelagicLarvaeState.Pelagic


  var polygon: HabitatPolygon = new GeometryAdaptor(null, 1, HabitatType.Reef)

  def this() = this(0, 0, 0, null, null, DateTime.now())

  def move(newPosition: GeoCoordinate): Unit = {
    if (newPosition.isValid) {
      debug("old position: " + position.toString)

      saveState()
      position = newPosition
      debug("new position: " + position.toString)

    } else {
      debug("new position is not valid")
    }
  }

  //lazy val maximumLifeSpan: Int = 0


}

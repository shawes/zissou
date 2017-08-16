package biology

import locals.OntogenyState.OntogenyState
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

class TimeCapsule(val age: Int,
                  val stage: OntogenyState,
                  val state: PelagicLarvaeState,
                  val habitat: Option[Int],
                  val position: GeoCoordinate) {}

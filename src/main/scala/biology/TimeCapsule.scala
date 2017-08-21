package biology

import locals.OntogenyState.OntogenyState
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate

class TimeCapsule(val age: Int,
                  val stage: OntogenyState,
                  val state: PelagicLarvaeState,
                  val habitat: Int,
                  val position: GeoCoordinate) {}

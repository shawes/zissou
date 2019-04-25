package biology

import locals._
import physical.GeoCoordinate

class TimeCapsule(
    val age: Int,
    val stage: OntogeneticState,
    val state: PelagicLarvaeState,
    val habitat: Int,
    val position: GeoCoordinate
) {}

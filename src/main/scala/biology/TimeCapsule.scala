package biology

import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate
import physical.habitat.HabitatPolygon

class TimeCapsule(val age: Int, val state: PelagicLarvaeState, val habitat: HabitatPolygon, val position: GeoCoordinate) {}

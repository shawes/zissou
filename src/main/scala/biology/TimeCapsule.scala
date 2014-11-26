package biology

import physical.habitat.HabitatPolygon
import locals.PelagicLarvaeState.PelagicLarvaeState
import physical.GeoCoordinate

class TimeCapsule(val age: Int, val state: PelagicLarvaeState, habitat: HabitatPolygon, position: GeoCoordinate) {}

package biology

import locals.Enums._
import physical.GeoCoordinate
import scala.collection.mutable.ArrayBuffer

trait History {
  val history: ArrayBuffer[TimeCapsule] = ArrayBuffer.empty[TimeCapsule]
}

class TimeCapsule(
    val age: Int,
    val stage: OntogeneticState,
    val state: PelagicLarvaeState,
    val habitat: Int,
    val position: GeoCoordinate
) {}

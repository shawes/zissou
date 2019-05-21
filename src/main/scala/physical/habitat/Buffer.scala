package physical.habitat

import org.locationtech.jts.geom.Geometry

class Buffer(
    val settlement: Double,
    val olfactory: Double
) {
  val settlementBufferShapes: List[Geometry] = List.empty
  val olfactoryBufferShapes: List[Geometry] = List.empty
  //bufferShapes.filter(x=>x.contains(g))
}

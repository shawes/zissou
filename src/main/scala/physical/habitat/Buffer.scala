package physical.habitat

import com.vividsolutions.jts.geom.Geometry

class Buffer(val isBuffered: Boolean, val settlement: Double, val olfactory: Double) {
  val settlementBufferShapes: List[Geometry] = List.empty
  val olfactoryBufferShapes: List[Geometry] = List.empty
  //bufferShapes.filter(x=>x.contains(g))
}

package physical.habitat

import com.vividsolutions.jts.geom.Geometry

class Buffer(val isBuffered: Boolean, val size: Double) {
  val bufferShapes: List[Geometry] = List.empty
  //bufferShapes.filter(x=>x.contains(g))
}

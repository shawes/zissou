package physical.habitat

import com.vividsolutions.jts.geom.Geometry

class Buffer(val isBuffered: Boolean, val settlement: Int, val olafactory: Int) {
  val settlementBufferShapes: List[Geometry] = List.empty
  val olafactoryBufferShapes: List[Geometry] = List.empty
  //bufferShapes.filter(x=>x.contains(g))
}

package physical.flow

import physical.{Velocity, GeoCoordinate}
import scala.collection.mutable.ArrayBuffer
import locals.Constants

class FlowPolygon(var id: Int,
                  var vertices: ArrayBuffer[GeoCoordinate],
                  var centroid: GeoCoordinate,
                  var velocity: Velocity,
                  var temperature: Double,
                  var salinity: Double,
                  var seaSurfaceHeight: Double,
                  var isLand: Boolean) {
  def this() = this(0, new ArrayBuffer(Constants.FlowPolygonVertices), new GeoCoordinate(), new Velocity(), 0, 0, 0, false)
}

package physical.habitat

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.algorithm.Angle
import locals.HabitatType
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor

trait HabitatPolygon {
  val id: Int
  val habitat: HabitatType
  def centroid: GeoCoordinate
  //def coordinates: Array[GeoCoordinate]
  def contains(coordinate: GeoCoordinate): Boolean
  def distance(coordinate: GeoCoordinate): Double
  def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean
  def direction(coordinate : GeoCoordinate) : Double
}

trait HabitatPolygonToJtsGeometryAdaptor {
  self: HabitatPolygon with GeometryAdaptor =>
}

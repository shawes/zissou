package physical.habitat

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.algorithm.Angle
import locals.Enums.HabitatType
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor

trait HabitatPolygon {
  val id: Int
  val habitat: HabitatType
  def centroid: GeoCoordinate
  // def coordinates: Array[GeoCoordinate]
  def contains(coordinate: GeoCoordinate): Boolean
  def distance(coordinate: GeoCoordinate): Double
  def isWithinBuffer(coordinate: GeoCoordinate, buffer: Double): Boolean
  def direction(coordinate: GeoCoordinate): Double
}

trait HabitatPolygonToJtsGeometryAdaptor {
  self: HabitatPolygon with GeometryAdaptor =>
}

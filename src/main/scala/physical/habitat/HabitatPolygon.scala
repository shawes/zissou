package physical.habitat

import com.vividsolutions.jts.geom.Geometry
import locals.HabitatType.HabitatType
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor

trait HabitatPolygon {
  val id: Int
  val habitat: HabitatType
  def centroid: GeoCoordinate
  def coordinates: Array[GeoCoordinate]
  def contains(coordinate: GeoCoordinate): Boolean
  def distance(coordinate: GeoCoordinate): Double
  def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean
}


trait HabitatPolygonToJtsGeometryAdaptor {
  self: HabitatPolygon with GeometryAdaptor =>
}

class GeometryAdaptor(val g: Geometry, val id: Int, val habitat: HabitatType) extends HabitatPolygon {


  def centroid: GeoCoordinate = GeometryToGeoCoordinateAdaptor.toGeoCoordinate(g.getCentroid)
  def coordinates: Array[GeoCoordinate] = g.getCoordinates.map(g => new GeoCoordinate(g.y, g.x))
  def contains(coordinate: GeoCoordinate): Boolean = g.contains(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))
  def distance(coordinate: GeoCoordinate): Double = g.distance(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))

  def intersects(coordinate: GeoCoordinate): Boolean = g.intersects(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))
  def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean =
    g.isWithinDistance(GeometryToGeoCoordinateAdaptor.toPoint(coordinate), distance)
}



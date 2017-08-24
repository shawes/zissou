package physical.habitat

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.algorithm.Angle
import locals.HabitatType.HabitatType
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor

class GeometryAdaptor(val g: Geometry, val id: Int, val habitat: HabitatType) extends HabitatPolygon with Ordered[GeometryAdaptor] {


  def centroid: GeoCoordinate = GeometryToGeoCoordinateAdaptor.toGeoCoordinate(g.getCentroid)
  //def coordinates: Array[GeoCoordinate] = g.getCoordinates.map(g => new GeoCoordinate(g.y, g.x))
  def contains(coordinate: GeoCoordinate): Boolean = g.contains(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))
  def distance(coordinate: GeoCoordinate): Double = g.distance(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))

  def intersects(coordinate: GeoCoordinate): Boolean = g.intersects(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))
  def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean =
    g.isWithinDistance(GeometryToGeoCoordinateAdaptor.toPoint(coordinate), distance)
  def direction(coordinate : GeoCoordinate) : Double = {
    Angle.toDegrees(Angle.angle(GeometryToGeoCoordinateAdaptor.toPoint(coordinate).getCoordinate,
    g.getCentroid.getCoordinate))
  }

  def compare(that: GeometryAdaptor): Int = {
    this.centroid.compare(that.centroid)
  }

}

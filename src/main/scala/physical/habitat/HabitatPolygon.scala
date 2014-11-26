package physical.habitat

import locals.HabitatType
import HabitatType.HabitatType
import physical.GeoCoordinate
import com.vividsolutions.jts.geom.Geometry

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
  /*
      def centroid: GeoCoordinate = centroid

      def coordinates: Array[GeoCoordinate] = g.getCoordinates.map(g => new GeoCoordinate(g.y, g.x))

      def contains(coordinate: GeoCoordinate): Boolean = g.contains(coordinate.toGeometry)

      def distance(coordinate: GeoCoordinate): Double = g.distance(coordinate.toGeometry)

      def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean = g.isWithinDistance(coordinate.toGeometry, distance)
  */

}

class GeometryAdaptor(val g: Geometry, val id: Int, val habitat: HabitatType) extends HabitatPolygon {


  def centroid: GeoCoordinate = new GeoCoordinate(g.getCentroid)

  def coordinates: Array[GeoCoordinate] = g.getCoordinates.map(g => new GeoCoordinate(g.y, g.x))

  def contains(coordinate: GeoCoordinate): Boolean = g.contains(coordinate.toGeometry)

  def distance(coordinate: GeoCoordinate): Double = g.distance(coordinate.toGeometry)

  def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean = g.isWithinDistance(coordinate.toGeometry, distance)
}



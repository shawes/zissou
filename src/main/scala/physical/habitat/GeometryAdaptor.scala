package physical.habitat

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.algorithm.Angle
import org.geotools.referencing.GeodeticCalculator
import locals.HabitatType
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor
import grizzled.slf4j.Logging
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.geometry.jts._
import org.locationtech.jts.operation.distance.DistanceOp

class GeometryAdaptor(val g: Geometry, val id: Int, val habitat: HabitatType) extends HabitatPolygon with Logging {


  def centroid: GeoCoordinate = GeometryToGeoCoordinateAdaptor.toGeoCoordinate(g.getCentroid)
  //def coordinates: Array[GeoCoordinate] = g.getCoordinates.map(g => new GeoCoordinate(g.y, g.x))
  def contains(coordinate: GeoCoordinate): Boolean = g.contains(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))

  // Returns distance in km
  // TODO: return units based on parameter (default is m)
  def distance(coordinate: GeoCoordinate): Double = {
    val crs = DefaultGeographicCRS.WGS84
    val geodeticCalc = new GeodeticCalculator()
    geodeticCalc.setStartingPosition(JTS.toDirectPosition(DistanceOp.nearestPoints(g, GeometryToGeoCoordinateAdaptor.toPoint(coordinate))(0),crs))
    geodeticCalc.setDestinationPosition( JTS.toDirectPosition ( GeometryToGeoCoordinateAdaptor.toPoint(coordinate).getCoordinate, crs))
    geodeticCalc.getOrthodromicDistance()/1000.0
  }

  def intersects(coordinate: GeoCoordinate): Boolean = g.intersects(GeometryToGeoCoordinateAdaptor.toPoint(coordinate))

  // A point on the polygon is at distance 0, so a buffer of 0 still holds it: the test must
  // be "at most", not "less than"
  def isWithinBuffer(coordinate: GeoCoordinate, buffer: Double): Boolean =
     distance(coordinate) <= buffer

  def direction(coordinate : GeoCoordinate) : Double = {
    Angle.toDegrees(Angle.angle(GeometryToGeoCoordinateAdaptor.toPoint(coordinate).getCoordinate,
    g.getCentroid.getCoordinate))
  }

}

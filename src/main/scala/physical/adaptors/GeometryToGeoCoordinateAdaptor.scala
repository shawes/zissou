package physical.adaptors

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}
import physical.GeoCoordinate

object GeometryToGeoCoordinateAdaptor {

  // As using WGS84, x = longitude and y = latitude
  def toPoint(coordinate: GeoCoordinate): Point = {
    new GeometryFactory().createPoint(new Coordinate(coordinate.longitude, coordinate.latitude))
  }
  
  def toGeoCoordinate(point: Point) = new GeoCoordinate(point.getCoordinate.y, point.getCoordinate.x, 0)
}

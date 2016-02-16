package physical.adaptors

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}
import physical.GeoCoordinate

object GeometryToGeoCoordinateAdaptor {

  def toPoint(coordinate: GeoCoordinate): Point = {
    new GeometryFactory().createPoint(new Coordinate(coordinate.latitude, coordinate.longitude))
  }

  def toGeoCoordinate(point: Point) = new GeoCoordinate(point.getCoordinate.y, point.getCoordinate.x, 0)
}

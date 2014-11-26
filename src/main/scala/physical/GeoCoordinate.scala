package physical

import scala.math.abs
import com.vividsolutions.jts.geom.{GeometryFactory, Coordinate, Point}

class GeoCoordinate(var latitude: Double, var longitude: Double, var depth: Double) extends Ordered[GeoCoordinate] {

  def this(lat: Double, lon: Double) = this(lat, lon, 0)

  def this(point: Point) = this(point.getCoordinate.y, point.getCoordinate.x, 0)

  def this() = this(0, 0, 0)

  def isUndefined: Boolean = longitude == Double.NaN || latitude == Double.NaN

  override def toString = "lat=" + latitude + ", lon=" + longitude + ", depth=" + depth

  //TODO Need to code the epsilon for doubles
  def compare(that: GeoCoordinate): Int = {
    abs(this.latitude.compare(abs(that.latitude))
      + abs(this.longitude.compare(that.longitude))
      + abs(this.depth.compare(that.depth)))
  }

  def toGeometry: Point = {
    new GeometryFactory().createPoint(new Coordinate(latitude, longitude))
  }
}

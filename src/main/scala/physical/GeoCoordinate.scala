package physical

import scala.math.abs

class GeoCoordinate(val latitude: Double, val longitude: Double, val depth: Double) extends Ordered[GeoCoordinate] {

  def this(lat: Double, lon: Double) = this(lat, lon, 0)
  def this() = this(0, 0, 0)

  def isUndefined: Boolean = longitude == Double.NaN || latitude == Double.NaN

  override def toString : String = "lat=" + latitude + ", lon=" + longitude + ", depth=" + depth

  //TODO Need to code the epsilon for doubles
  def compare(that: GeoCoordinate): Int = {
    abs(this.latitude.compare(abs(that.latitude))
      + abs(this.longitude.compare(that.longitude))
      + abs(this.depth.compare(that.depth)))
  }


}

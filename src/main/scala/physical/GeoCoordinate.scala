package physical

class GeoCoordinate(val latitude: Double, val longitude: Double, val depth: Double) extends Ordered[GeoCoordinate] {

  def this(lat: Double, lon: Double) = this(lat, lon, 0)
  def this() = this(0, 0, 0)

  def isValid: Boolean = !isUndefined

  private def isUndefined: Boolean = latitude.isNaN || longitude.isNaN

  override def toString : String = "lat=" + f"$latitude%1.5f" + ", lon=" + f"$longitude%1.5f" + ", depth=" + depth

  //TODO Need to code the epsilon for doubles
  def compare(that: GeoCoordinate): Int = {
    (this.latitude * that.longitude).toInt - (that.latitude * this.longitude).toInt
  }


}

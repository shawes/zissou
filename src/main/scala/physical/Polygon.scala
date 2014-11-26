package physical


trait Polygon {
  def centroid: GeoCoordinate

  def coordinates: Array[GeoCoordinate]

  def contains(coordinate: GeoCoordinate): Boolean

  def distance(coordinate: GeoCoordinate): Double

  def isWithinDistance(coordinate: GeoCoordinate, distance: Double): Boolean
}


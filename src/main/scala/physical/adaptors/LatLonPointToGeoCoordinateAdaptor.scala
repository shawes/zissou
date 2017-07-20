package physical.adaptors

import physical.GeoCoordinate
import ucar.unidata.geoloc.LatLonPoint

object LatLonPointToGeoCoordinateAdaptor {
  def toGeoCoordinate(point: LatLonPoint) = new GeoCoordinate(point.getLatitude, point.getLongitude, 0)
}

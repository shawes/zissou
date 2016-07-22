package maths.interpolation

import grizzled.slf4j.Logging
import locals.Interpolation._
import maths.interpolation.cubic.BicubicInterpolation
import physical.flow.FlowGridWrapper
import physical.{GeoCoordinate, Velocity}

class Interpolation extends Logging {

  //TODO: Get dimensions from flow grid
  def apply(coordinate: GeoCoordinate, polygons: FlowGridWrapper, index: Array[Int]): Velocity = {
    //debug("Interpolating the coordinate " + coordinate)
    val centroid = polygons.getCentroid(index)
    //debug("Retrieved the polygon " + polygon.id)
    val latitudeDisplacement = (coordinate.latitude - centroid.latitude) * (1.0 / 0.1) + 1.0
    val longitudeDisplacement = (coordinate.longitude - centroid.longitude) * (1.0 / 0.1) + 1.0
    //debug("Latitude displacement = " + latitudeDisplacement + ", longitude displacement = " + longitudeDisplacement)

    val bicubicInterpolation = new BicubicInterpolation
    val neighbourhood = polygons.getInterpolationValues(coordinate, Bicubic)
    bicubicInterpolation.interpolate(neighbourhood, longitudeDisplacement, latitudeDisplacement)

  }
}



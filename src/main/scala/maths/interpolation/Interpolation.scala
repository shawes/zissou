package maths.interpolation

import exceptions.UndefinedVelocityException
import grizzled.slf4j.Logging
import locals.InterpolationType._
import maths.interpolation.cubic.BicubicInterpolation
import maths.interpolation.linear.BilinearInterpolation
import physical.flow.FlowGridWrapper
import physical.{GeoCoordinate, Velocity}

class Interpolation extends Logging {

  //TODO: Get dimensions from flow grid
  def apply(coordinate: GeoCoordinate, grid: FlowGridWrapper, index: Array[Int]): Velocity = {
    //debug("Interpolating the coordinate " + coordinate)
    val centroid = grid.getCentroid(index)
    //debug("Retrieved the polygon " + polygon.id)
    val latitudeDisplacement = (coordinate.latitude - centroid.latitude) * (1.0 / 0.1) + 1.0
    val longitudeDisplacement = (coordinate.longitude - centroid.longitude) * (1.0 / 0.1) + 1.0
    //debug("Latitude displacement = " + latitudeDisplacement + ", longitude displacement = " + longitudeDisplacement)

    val bicubicInterpolation = new BicubicInterpolation()

    val neighbourhood = grid.getInterpolationValues(coordinate, Bicubic)
    if (neighbourhood.nonEmpty) {
      bicubicInterpolation(neighbourhood, longitudeDisplacement, latitudeDisplacement)
    } else {
      val bilinearInterpolation = new BilinearInterpolation()
      val neighbourhood = grid.getInterpolationValues(coordinate, Bilinear)
      if (neighbourhood.nonEmpty) {
        bilinearInterpolation(neighbourhood, longitudeDisplacement, latitudeDisplacement)
      } else {
        grid.getVelocity(index)
      }
    }
  }

  private def nextInterpolationStep(interpolation: Interpolation): InterpolationType = interpolation match {
    case Bicubic => Bilinear
    case Tricubic => TriLinear
    case _ => throw new UndefinedVelocityException()
  }
}



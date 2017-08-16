package maths.interpolation

import grizzled.slf4j.Logging
import locals.InterpolationType._
import maths.interpolation.cubic.BicubicInterpolation
import maths.interpolation.linear.BilinearInterpolation
import physical.flow.FlowGridWrapper
import physical.{GeoCoordinate, Velocity}

class Interpolation extends Logging {


  val bicubicInterpolation = new BicubicInterpolation()
  val bilinearInterpolation = new BilinearInterpolation()
  //TODO: Get dimensions from flow grid
  def apply(coordinate: GeoCoordinate, grid: FlowGridWrapper, index: Array[Int]): Option[Velocity] = {
    this.synchronized {
      val centroid = grid.getCentroid(index)
      val latitudeDisplacement = (coordinate.latitude - centroid.latitude) * (1.0 / 0.1) + 1.0
      val longitudeDisplacement = (coordinate.longitude - centroid.longitude) * (1.0 / 0.1) + 1.0

      val neighbourhood = grid.getInterpolationValues(coordinate, Bicubic)
      if (neighbourhood.isDefined) {
        Some(interpolate(Bicubic, neighbourhood.get, longitudeDisplacement, latitudeDisplacement))
      } else {
        val neighbourhood = grid.getInterpolationValues(coordinate, Bilinear)
        if (neighbourhood.isDefined) {
          Some(interpolate(Bilinear, neighbourhood.get, longitudeDisplacement, latitudeDisplacement))
        } else {
          grid.getVelocity(index)
        }
      }
    }
  }

  private def interpolate(interpolation: InterpolationType, neighbourhood: Array[Array[Velocity]],
                          long: Double, lat: Double): Velocity = interpolation match {
    case Bicubic => bicubicInterpolation(neighbourhood, long, lat)
    case Bilinear => bilinearInterpolation(neighbourhood, long, lat)
    case _ => throw new RuntimeException("Undefined interpolation method")
  }

  // TODO: Implement tricubic interpolation
  // private def nextInterpolationStep(interpolation: InterpolationType): InterpolationType = interpolation match {
  //   case Bicubic => Bilinear
  //   case Tricubic => TriLinear
  //   case _ => throw new RuntimeException("Undefined interpolation method")
  // }
}

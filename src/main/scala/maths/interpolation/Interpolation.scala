package maths.interpolation

import grizzled.slf4j.Logging
import locals._
import locals.Enums.InterpolationType
import maths.interpolation.cubic.BicubicInterpolation
import maths.interpolation.linear.BilinearInterpolation
import physical.flow.FlowGridWrapper
import physical.{GeoCoordinate, Velocity}

class Interpolation extends Logging {

  val bicubicInterpolation = new BicubicInterpolation()
  val bilinearInterpolation = new BilinearInterpolation()
  // TODO: Get dimensions from flow grid
  def apply(
      coordinate: GeoCoordinate,
      grid: FlowGridWrapper,
      index: (Int, Int, Int, Boolean)
  ): Option[Velocity] = {
    val centroid = grid.getCentroid(index)
    debug("Centroid of the grid is: " + centroid)
    val latitudeDisplacement =
      math.abs(coordinate.latitude - centroid.latitude) * (1.0 / 0.1) + 1.0
    val longitudeDisplacement = math.abs(
      coordinate.longitude - centroid.longitude
    ) * (1.0 / 0.1) + 1.0
    debug(
      "Latitude : " + latitudeDisplacement + ", long: " + longitudeDisplacement
    )

    val neighbourhood =
      grid.getInterpolationValues(coordinate, InterpolationType.Bicubic)
    if (neighbourhood.isDefined) then {
      debug("Can bicubic interpolate")
      Some(
        interpolate(
          InterpolationType.Bicubic,
          neighbourhood.get,
          longitudeDisplacement,
          latitudeDisplacement
        )
      )
    } else {
      val neighbourhood =
        grid.getInterpolationValues(coordinate, InterpolationType.Bilinear)
      if (neighbourhood.isDefined) then {
        debug("Can only bilnear interpolate")
        Some(
          interpolate(
            InterpolationType.Bilinear,
            neighbourhood.get,
            longitudeDisplacement,
            latitudeDisplacement
          )
        )
      } else {
        debug("No interpolations")
        grid.getVelocity(index)
      }
    }
  }

  private def interpolate(
      interpolation: InterpolationType,
      neighbourhood: Array[Array[Velocity]],
      long: Double,
      lat: Double
  ): Velocity = interpolation match {
    case InterpolationType.Bicubic =>
      bicubicInterpolation(neighbourhood, long, lat)
    case InterpolationType.Bilinear =>
      bilinearInterpolation(neighbourhood, long, lat)
    // case Tricubic => new tricubicInterpolation(neighbourhood, long, lat)
    // case Trilinear => new trilinearInterpolation(neighbourhood, long, lat)
    case _ => throw new RuntimeException("Undefined interpolation method")
  }

  // TODO: Implement tricubic interpolation
  // private def nextInterpolationStep(interpolation: InterpolationType): InterpolationType = interpolation match {
  //   case Bicubic => Bilinear
  //   case Tricubic => TriLinear
  //   case _ => throw new RuntimeException("Undefined interpolation method")
  // }
}

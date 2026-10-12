package maths.interpolation

import grizzled.slf4j.Logging
import locals._
import maths.interpolation.cubic.BicubicInterpolation
import maths.interpolation.linear.BilinearInterpolation
import physical.flow.FlowGridWrapper
import physical.{GeoCoordinate, Velocity}

/**
  * The flow at a position between grid nodes: bicubic where the 4 by 4 nodes round it all have
  * flow, bilinear where only the 2 by 2 do, and the nearest node's flow where they do not.
  */
class Interpolation extends Logging {

  val bicubicInterpolation = new BicubicInterpolation()
  val bilinearInterpolation = new BilinearInterpolation()

  def apply(
      coordinate: GeoCoordinate,
      grid: FlowGridWrapper,
      index: (Int, Int, Int, Boolean)
  ): Option[Velocity] =
    grid
      .stencil(coordinate, Bicubic, index)
      .map(s => bicubicInterpolation(s.nodes, s.longitudeFraction, s.latitudeFraction))
      .orElse(
        grid
          .stencil(coordinate, Bilinear, index)
          .map(s => bilinearInterpolation(s.nodes, s.longitudeFraction, s.latitudeFraction))
      )
      .orElse(grid.getVelocity(index))
}

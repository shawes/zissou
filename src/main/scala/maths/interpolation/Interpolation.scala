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
    if (grid.colocated) onOneGrid(coordinate, grid, index) else onGridsOfTheirOwn(coordinate, grid, index._4)

  // u, v and w on grids of their own, as in BRAN2020: each is interpolated among the nodes of its own
  // grid, and there is flow only where all three have it
  private def onGridsOfTheirOwn(coordinate: GeoCoordinate, grid: FlowGridWrapper, isEarlier: Boolean): Option[Velocity] = {
    val u = component(0, coordinate, grid, isEarlier)
    val v = component(1, coordinate, grid, isEarlier)
    val w = component(2, coordinate, grid, isEarlier)
    if (u.isEmpty || v.isEmpty || w.isEmpty) None else Some(new Velocity(u.get, v.get, w.get))
  }

  private def component(c: Int, coordinate: GeoCoordinate, grid: FlowGridWrapper, isEarlier: Boolean): Option[Double] =
    grid
      .componentStencil(c, coordinate, Bicubic, isEarlier)
      .map(s => bicubicInterpolation(s.nodes, s.longitudeFraction, s.latitudeFraction).u)
      .orElse(
        grid
          .componentStencil(c, coordinate, Bilinear, isEarlier)
          .map(s => bilinearInterpolation(s.nodes, s.longitudeFraction, s.latitudeFraction).u)
      )
      .orElse(grid.componentNearest(c, coordinate, isEarlier))

  private def onOneGrid(coordinate: GeoCoordinate, grid: FlowGridWrapper, index: (Int, Int, Int, Boolean)): Option[Velocity] =
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

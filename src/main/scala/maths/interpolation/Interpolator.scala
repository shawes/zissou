package maths.interpolation

import maths.interpolation.cubic.BicubicInterpolator
import maths.interpolation.linear.BilinearInterpolator
import physical.flow.{Flow, FlowPolygon}
import physical.{GeoCoordinate, Velocity}

import scala.collection.mutable.ArrayBuffer

class Interpolator(flow: Flow) {

  def interpolate(coordinate: GeoCoordinate, polygons: Array[FlowPolygon], index: Int): Velocity = {

    val polygon = polygons(index)
    val latitudeDisplacement = (coordinate.latitude - polygon.vertices(0).latitude) * (1.0 / flow.grid.cell.height) + 1.0
    val longitudeDisplacement = (coordinate.longitude - polygon.vertices(0).longitude) * (1.0 / flow.grid.cell.width) + 1.0


    var result = bicubicInterpolation(polygons, index, longitudeDisplacement, latitudeDisplacement)

    if (!result._1) result = bilinearInterpolation(polygons, index, longitudeDisplacement, latitudeDisplacement)

    result._2

  }


  private def bicubicInterpolation(polygons: Array[FlowPolygon], index: Int, longitude: Double, latitude: Double): Tuple2[Boolean, Velocity] = {
    val bicubicInterpolator = new BicubicInterpolator
    val neighbourhood = new ArrayBuffer[Array[Velocity]]()
    val neighbourhoodWidth = flow.grid.width

    for (j <- -2 to 1) {
      val row = new ArrayBuffer[Velocity]()
      for (i <- -1 to 2) {
        val neighbouringVelocity = polygons((i + index) + j * neighbourhoodWidth).velocity
        if (neighbouringVelocity.isUndefined) return new Tuple2(false, neighbouringVelocity)
        row += neighbouringVelocity
      }
      neighbourhood += row.toArray
    }
    val interpolatedVelocity = bicubicInterpolator.interpolate(neighbourhood.toArray, longitude, latitude)
    new Tuple2(true, interpolatedVelocity)
  }

  private def bilinearInterpolation(polygons: Array[FlowPolygon], index: Int, longitude: Double, latitude: Double): Tuple2[Boolean, Velocity] = {
    val bilinearInterpolator = new BilinearInterpolator
    val neighbourhood = new ArrayBuffer[Array[Velocity]]()
    val neighbourhoodWidth = flow.grid.width

    for (j <- 0 to 1) {
      val row = new ArrayBuffer[Velocity]()
      for (i <- 1 to 2) {
        val neighbouringVelocity = polygons((i + index) + j * neighbourhoodWidth).velocity
        if (neighbouringVelocity.isUndefined) return new Tuple2(false, neighbouringVelocity)
        row += neighbouringVelocity
      }
      neighbourhood += row.toArray
    }
    val interpolatedVelocity = bilinearInterpolator.interpolate(neighbourhood.toArray, longitude, latitude)
    new Tuple2(true, interpolatedVelocity)
  }
}



package maths.interpolation

import grizzled.slf4j.Logging
import maths.interpolation.cubic.BicubicInterpolator
import maths.interpolation.linear.BilinearInterpolator
import physical.flow.{Dimensions, FlowGridWrapper}
import physical.{GeoCoordinate, Velocity}

import scala.collection.mutable.ArrayBuffer

class Interpolator(var dim: Dimensions) extends Logging {

  def interpolate(coordinate: GeoCoordinate, polygons: FlowGridWrapper, index: Array[Int]): Velocity = {
    //debug("Interpolating the coordinate " + coordinate)
    val centroid = polygons.getCentroid(index)
    //debug("Retrieved the polygon " + polygon.id)
    val latitudeDisplacement = (coordinate.latitude - centroid.latitude) * (1.0 / dim.cellSize.cell.height) + 1.0
    val longitudeDisplacement = (coordinate.longitude - centroid.longitude) * (1.0 / dim.cellSize.cell.width) + 1.0
    //debug("Latitude displacement = " + latitudeDisplacement + ", longitude displacement = " + longitudeDisplacement)

    var result = bicubicInterpolation(polygons, index, longitudeDisplacement, latitudeDisplacement)
    //debug("Bi-cubic interpolation = " + result._2)

    if (!result._1) {
      result = bilinearInterpolation(polygons, index, longitudeDisplacement, latitudeDisplacement)
      //debug("Bi-linear interpolation = " + result._2)
    }
    result._2

  }


  private def bicubicInterpolation(polygons: FlowGridWrapper, index: Array[Int], longitude: Double, latitude: Double): Tuple2[Boolean, Velocity] = {
    val bicubicInterpolator = new BicubicInterpolator
    val neighbourhood = ArrayBuffer.empty[ArrayBuffer[Velocity]]
    val neighbourhoodWidth = dim.cellSize.width
    for (j <- -1 to 2) {
      for (i <- -1 to 2) {
        val neighbouringVelocity = polygons.getVelocity(Array(index(0) + i, index(1) + j, index(2)))
        if (neighbouringVelocity.isUndefined) return new Tuple2(false, neighbouringVelocity)
        neighbourhood :+ neighbouringVelocity
      }
    }

    val interpolatedVelocity = bicubicInterpolator.interpolate(neighbourhood, longitude, latitude)
    new Tuple2(true, interpolatedVelocity)
  }

  private def bilinearInterpolation(polygons: FlowGridWrapper, index: Array[Int], longitude: Double, latitude: Double): Tuple2[Boolean, Velocity] = {
    val bilinearInterpolator = new BilinearInterpolator
    val neighbourhood = new ArrayBuffer[ArrayBuffer[Velocity]]()
    val neighbourhoodWidth = dim.cellSize.width

    for (j <- 0 to 1) {
      for (i <- 0 to 1) {
        val neighbouringVelocity = polygons.getVelocity(Array(index(0) + i, index(1) + j, index(2)))
        if (neighbouringVelocity.isUndefined) return new Tuple2(false, neighbouringVelocity)
        neighbourhood :+ neighbouringVelocity
      }
    }
    val interpolatedVelocity = bilinearInterpolator.interpolate(neighbourhood, longitude, latitude)
    new Tuple2(true, interpolatedVelocity)
  }
}



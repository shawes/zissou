package maths

import locals.Constants
import physical.{GeoCoordinate, Velocity}

class Geometry {

  def translatePoint(point: GeoCoordinate, velocity: Velocity,
                     timeStep: Int, swimming: Double): GeoCoordinate  = {

    if (velocity.isUndefined) return point

    val xDistance = (velocity.u + swimming.u) * timeStep
    val yDistance = (velocity.v + swimming.v) * timeStep

    val kmLatitude = (-0.00000344188 * math.pow(point.latitude, 3)) + (.000466 * math.pow(point.latitude, 2)) +
      (-0.001537 * point.latitude) + 110.572356
    val kmLongitude = (0.000068 * Math.pow(point.latitude, 3)) + (-0.020724 * Math.pow(point.latitude, 2)) +
      (0.08253 * point.latitude) + 110.806595

    //val depth = ceilingDepth(point.depth + (velocity.w * timeStep))

    new GeoCoordinate(point.latitude + (xDistance / (kmLatitude * 1000)),
      point.longitude + (yDistance / (kmLongitude * 1000)), point.depth)
  }

  /**
    *
    * Returns units of metres
    *
    * @param p1
    * @param p2
    * @return
    */
  def getDistanceBetweenTwoPoints(p1: GeoCoordinate, p2: GeoCoordinate): Double = {

    val lat1 = Math.toRadians(p1.latitude)
    val lat2 = Math.toRadians(p2.latitude)
    val lon1 = Math.toRadians(p1.longitude)
    val lon2 = Math.toRadians(p2.longitude)

    Math.acos(Math.sin(lat1) * Math.sin(lat2) +
      Math.cos(lat1) * Math.cos(lat2) *
        Math.cos(lon2 - lon1)) * Constants.EarthsRadius
  }

  private def ceilingDepth(depth: Double): Double = {
    var newDepth = depth
    if (depth < 0) newDepth = 0
    if (depth > 90) newDepth = 90
    depth
  }


}

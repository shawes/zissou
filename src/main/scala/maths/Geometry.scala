package maths

import locals.Constants
import physical.{GeoCoordinate, Velocity}
import grizzled.slf4j.Logging

class Geometry extends Logging {

  def translatePoint(point: GeoCoordinate, velocity: Velocity,
                     timeStep: Int, swimming: Velocity): GeoCoordinate  = {

    if (velocity.isUndefined) return point

    val xDistance = (velocity.u + swimming.u) * timeStep
    val yDistance = (velocity.v + swimming.v) * timeStep
    val zDistance = velocity.w * timeStep

    val newLatitude  = point.latitude  + (yDistance / Constants.EarthsRadius) * (180 / Math.PI)
    val newLongitude = point.longitude + (xDistance / Constants.EarthsRadius) * (180 / Math.PI) / Math.cos(point.latitude*Math.PI/180)

    new GeoCoordinate(newLatitude, newLongitude, point.depth + zDistance)
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

  def getAngleBetweenTwoPoints(p1: GeoCoordinate, p2: GeoCoordinate): Double = {
    Math.atan2(p2.latitude - p1.latitude, p2.longitude - p1.longitude)
}

  private def ceilingDepth(depth: Double): Double = {
    var newDepth = depth
    if (depth < 0) newDepth = 0
    if (depth > 90) newDepth = 90
    depth
  }


}

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

    new GeoCoordinate(newLatitude, newLongitude, ceilingDepth(point.depth + zDistance))
  }

  def translatePoint2(point: GeoCoordinate, velocity: Velocity,
                     timeStep: Int, swimming: Velocity): GeoCoordinate  = {

    if (velocity.isUndefined) return point

    val xDistance = (velocity.u + swimming.u) * timeStep
    val yDistance = (velocity.v + swimming.v) * timeStep
    val zDistance = velocity.w * timeStep

  val kmLatitude = (-0.00000344188 * math.pow(point.latitude, 3)) + (0.000466 * math.pow(point.latitude, 2)) + (-0.001537 * point.latitude) + 110.572356
  val kmLongitude = (0.000068 * Math.pow(point.latitude, 3)) + (-0.020724 * Math.pow(point.latitude, 2)) + (0.08253 * point.latitude) + 110.806595
  val newLatitude = point.latitude+xDistance/(kmLatitude*1000)
  val newLongitude = point.longitude+yDistance/(kmLongitude*1000)
  new GeoCoordinate(newLatitude, newLongitude, ceilingDepth(point.depth + zDistance))

}

def translatePoint3(point: GeoCoordinate, velocity: Velocity,
                   timeStep: Int, swimming: Velocity): GeoCoordinate  = {

  if (velocity.isUndefined) return point

  val xDistance = (velocity.u + swimming.u) * timeStep
  val yDistance = (velocity.v + swimming.v) * timeStep
  val zDistance = velocity.w * timeStep
  val longitudeInRadians = Math.toRadians(point.longitude)
  val latitudeInRadians = Math.toRadians(point.latitude)
  //rln1=deg2rad*lon_old
  //rlt1=deg2rad*lat_old
  //rlt2=asin(sin(rlt1+dy*REINV)*cos(dx*REINV))

  val newLatitudeInRadians = Math.asin(
    Math.sin(latitudeInRadians + (yDistance / Constants.EarthsRadius)) *
    Math.cos(xDistance / Constants.EarthsRadius))

  //dlon=atan2(sin(dx*REINV)*cos(rlt1),(cos(dx*REINV)-sin(rlt1)*sin(rlt2)))

  val longitudeDisplacement = Math.atan2(Math.sin(xDistance/Constants.EarthsRadius) * Math.cos(latitudeInRadians),Math.cos(xDistance / Constants.EarthsRadius) - Math.sin(latitudeInRadians) * Math.sin(newLatitudeInRadians) )

  //lon_new=(rln1+dlon)*rad2deg;
  //lat_new=rlt2*rad2deg;
  //depth_new=depth_old+dz

  val newLongitude = Math.toDegrees(longitudeInRadians + longitudeDisplacement)
  val newLatitude = Math.toDegrees(newLatitudeInRadians)
  new GeoCoordinate(newLatitude, newLongitude, ceilingDepth(point.depth + zDistance))

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

  def getDistanceBetweenTwoPoints2(p1: GeoCoordinate, p2: GeoCoordinate): Double = {

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
    if (depth > 100) newDepth = 100
    newDepth
  }


}

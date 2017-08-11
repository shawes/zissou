package maths

import locals.Constants
import physical.{GeoCoordinate, Velocity}
import grizzled.slf4j.Logging

class Geometry extends Logging {

  def translatePoint(point: GeoCoordinate, velocity: Velocity,
                     timeStep: Int, swimming: Velocity): GeoCoordinate  = {

    if (velocity.isUndefined) return point
    debug(point)
    debug(velocity)
    debug(swimming)
    debug("timestep="+timeStep)

    val yVelocity = velocity.v + swimming.v
    debug("yV="+yVelocity)
    val xDistance = (velocity.u + swimming.u) * timeStep
    val yDistance = (yVelocity) * timeStep
    val zDistance = velocity.w * timeStep
    debug("x: " + xDistance + ",y=" +yDistance +",z="+zDistance)

    val kmLatitude = (-0.00000344188 * math.pow(point.latitude, 3)) + (0.000466 * math.pow(point.latitude, 2)) + (-0.001537 * point.latitude) + 110.572356
    val kmLongitude = (0.000068 * Math.pow(point.latitude, 3)) + (-0.020724 * Math.pow(point.latitude, 2)) + (0.08253 * point.latitude) + 110.806595
    val nlat = point.latitude+xDistance/(kmLatitude*1000)
    val nlon = point.longitude+yDistance/(kmLongitude*1000)

    val newLatitude  = point.latitude  + (yDistance / Constants.EarthsRadius) * (180 / Math.PI)
    val newLongitude = point.longitude + (xDistance / Constants.EarthsRadius) * (180 / Math.PI) / Math.cos(point.latitude*Math.PI/180)
    debug("Calc1 lat="+nlat+", lon="+nlon)
    debug("Calc2 lat="+newLatitude+", lon="+newLongitude)

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

    val newLongitude2 = Math.toDegrees(longitudeInRadians + longitudeDisplacement)
    val newLatitude2 = Math.toDegrees(newLatitudeInRadians)

debug("Calc3 lat="+newLatitude2+", lon="+newLongitude2)

    //val depth = ceilingDepth(point.depth + (velocity.w * timeStep))

    new GeoCoordinate(newLatitude2, newLongitude2, point.depth)
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

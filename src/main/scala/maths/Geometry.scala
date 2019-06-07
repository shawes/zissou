package maths

import locals.Constants
import physical.{GeoCoordinate, Velocity}
import grizzled.slf4j.Logging

class Geometry() extends Logging {

  def translatePoint(
      point: GeoCoordinate,
      velocity: Velocity,
      timeStep: Int,
      swimming: Velocity,
      includeVerticalVelocity: Boolean,
      backtracking: Boolean
  ): GeoCoordinate = {

    if (velocity.isUndefined) return point

    val distances = calculateDistance(
      velocity,
      timeStep,
      swimming,
      includeVerticalVelocity,
      backtracking
    )
    val newLatitude = point.latitude + (distances._2 / Constants.EarthsRadius) * (180 / Math.PI)
    val newLongitude =
      point.longitude + (distances._1 / Constants.EarthsRadius) * (180 / Math.PI) /
        Math.cos(point.latitude * Math.PI / 180)

    new GeoCoordinate(
      newLatitude,
      newLongitude,
      ceilingDepth(point.depth + distances._3)
    )
  }

  def translatePointApproximation(
      point: GeoCoordinate,
      velocity: Velocity,
      timeStep: Int,
      swimming: Velocity,
      includeVerticalVelocity: Boolean,
      backtracking: Boolean
  ): GeoCoordinate = {

    if (velocity.isUndefined) return point

    val distances = calculateDistance(
      velocity,
      timeStep,
      swimming,
      includeVerticalVelocity,
      backtracking
    )

    val kmLatitude = (-0.00000344188 * math.pow(point.latitude, 3)) + (0.000466 * math
      .pow(point.latitude, 2)) + (-0.001537 * point.latitude) + 110.572356
    val kmLongitude = (0.000068 * Math.pow(point.latitude, 3)) + (-0.020724 * Math
      .pow(point.latitude, 2)) + (0.08253 * point.latitude) + 110.806595
    val newLatitude = point.latitude + distances._2 / (kmLatitude * 1000)
    val newLongitude = point.longitude + distances._1 / (kmLongitude * 1000)
    new GeoCoordinate(
      newLatitude,
      newLongitude,
      ceilingDepth(point.depth + distances._3)
    )

  }

  def translatePointPrecision(
      point: GeoCoordinate,
      velocity: Velocity,
      timeStep: Int,
      swimming: Velocity,
      includeVerticalVelocity: Boolean,
      backtracking: Boolean
  ): GeoCoordinate = {

    if (velocity.isUndefined) return point

    val distances =
      calculateDistance(
        velocity,
        timeStep,
        swimming,
        includeVerticalVelocity,
        backtracking
      )
    val longitudeInRadians = Math.toRadians(point.longitude)
    val latitudeInRadians = Math.toRadians(point.latitude)

    val newLatitudeInRadians = Math.asin(
      Math.sin(latitudeInRadians + (distances._2 / Constants.EarthsRadius)) *
        Math.cos(distances._1 / Constants.EarthsRadius)
    )

    val longitudeDisplacement = Math.atan2(
      Math.sin(distances._1 / Constants.EarthsRadius) * Math.cos(
        latitudeInRadians
      ),
      Math.cos(distances._1 / Constants.EarthsRadius) - Math
        .sin(latitudeInRadians) * Math.sin(newLatitudeInRadians)
    )

    val newLongitude =
      Math.toDegrees(longitudeInRadians + longitudeDisplacement)
    val newLatitude = Math.toDegrees(newLatitudeInRadians)
    new GeoCoordinate(
      newLatitude,
      newLongitude,
      ceilingDepth(point.depth + distances._3)
    )

  }

  /**
    *
    * Returns units of metres
    *
    * @param p1
    * @param p2
    * @return
    */
  def getDistanceBetweenTwoPoints(
      p1: GeoCoordinate,
      p2: GeoCoordinate
  ): Double = {

    val lat1 = Math.toRadians(p1.latitude)
    val lat2 = Math.toRadians(p2.latitude)
    val lon1 = Math.toRadians(p1.longitude)
    val lon2 = Math.toRadians(p2.longitude)

    Math.acos(
      Math.sin(lat1) * Math.sin(lat2) +
        Math.cos(lat1) * Math.cos(lat2) *
          Math.cos(lon2 - lon1)
    ) * Constants.EarthsRadius
  }

  def getDistanceBetweenTwoPoints2(
      p1: GeoCoordinate,
      p2: GeoCoordinate
  ): Double = {

    val lat1 = Math.toRadians(p1.latitude)
    val lat2 = Math.toRadians(p2.latitude)
    val lon1 = Math.toRadians(p1.longitude)
    val lon2 = Math.toRadians(p2.longitude)

    Math.acos(
      Math.sin(lat1) * Math.sin(lat2) +
        Math.cos(lat1) * Math.cos(lat2) *
          Math.cos(lon2 - lon1)
    ) * Constants.EarthsRadius
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

  private def calculateDistance(
      velocity: Velocity,
      timeStep: Int,
      swimming: Velocity,
      includeVerticalVelocity: Boolean,
      backtracking: Boolean
  ): (Double, Double, Double) = {
    val xDistance = {
      val distance = (velocity.u + swimming.u) * timeStep
      if (backtracking) applyBacktracking(distance) else distance
    }
    val yDistance = {
      val distance = (velocity.v + swimming.v) * timeStep
      if (backtracking) applyBacktracking(distance) else distance
    }
    val zDistance = {
      val distance = if (includeVerticalVelocity) velocity.w * timeStep else 0
      if (backtracking) applyBacktracking(distance) else distance
    }
    (xDistance, yDistance, zDistance)
  }

  private def applyBacktracking(value: Double): Double = -1 * value

}

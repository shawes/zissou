package maths

import physical.{GeoCoordinate, Velocity}

class Geometry {

  val EarthsRadius = 6378137.0

  def translatePoint(point: GeoCoordinate, velocity: Velocity,
                     timeStep: Int): GeoCoordinate = translatePoint(point, velocity, timeStep, 0)

  def translatePoint(point: GeoCoordinate, velocity: Velocity,
                     timeStep: Int, speed: Double): GeoCoordinate = polynomialTranslation(point, velocity, timeStep, speed)

  private def polynomialTranslation(point: GeoCoordinate, velocity: Velocity,
                                    timeStep: Int, speed: Double): GeoCoordinate = {

    if (velocity.isUndefined) return point

    val xDistance = (velocity.u + speed) * timeStep
    val yDistance = (velocity.v + speed) * timeStep

    val kmLatitude = (-0.00000344188 * math.pow(point.latitude, 3)) + (.000466 * math.pow(point.latitude, 2)) +
      (-0.001537 * point.latitude) + 110.572356
    val kmLongitude = (0.000068 * Math.pow(point.latitude, 3)) + (-0.020724 * Math.pow(point.latitude, 2)) +
      (0.08253 * point.latitude) + 110.806595

    val depth = ceilingDepth(point.depth + (velocity.w * timeStep))

    //depth = adjustDepth(depth)


    new GeoCoordinate(point.latitude + (xDistance / (kmLatitude * 1000)),
      point.longitude + (yDistance / (kmLongitude * 1000)), depth)
  }

  private def ceilingDepth(depth: Double): Double = {
    var newDepth = depth
    if (depth < 0) newDepth = 0
    if (depth > 90) newDepth = 90
    depth
  }


}

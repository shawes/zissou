package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.FlowFileIterator
import locals.Enums.InterpolationTime
import locals.Constants
import maths.RandomNumberGenerator
import maths.interpolation.Interpolation
import physical.{GeoCoordinate, Velocity}

class FlowController(val reader: FlowFileIterator, var flow: Flow)
    extends Logging {

  private var hydrodynamicFlow = reader.next()
  flow.dimensions = reader.flow.dimensions

  def getVelocityOfCoordinate(
      coordinate: GeoCoordinate,
      future: LocalDateTime,
      now: LocalDateTime,
      timeStep: Int
  ): Option[Velocity] = {
    if (future == now) then {
      getInterpolatedVelocity(coordinate, InterpolationTime.Today)
    } else if (future == now.plusSeconds(timeStep)) then {
      getInterpolatedVelocity(coordinate, InterpolationTime.Tomorrow)
    } else {
      derivePartialTimeStepVelocity(coordinate, future, now, timeStep)
    }
  }

  private def derivePartialTimeStepVelocity(
      coordinate: GeoCoordinate,
      future: LocalDateTime,
      now: LocalDateTime,
      timeStep: Int
  ): Option[Velocity] = {
    val velocityNow =
      getInterpolatedVelocity(coordinate, InterpolationTime.Today)
    val velocityFuture =
      getInterpolatedVelocity(coordinate, InterpolationTime.Tomorrow)

    if (velocityNow.isDefined && velocityFuture.isDefined) then {
      val divisor = 1 / timeStep.toDouble
      val period = now.toDateTime(DateTimeZone.UTC) to future.toDateTime(
        DateTimeZone.UTC
      )

      val ratioA = period.toPeriod.getSeconds
      val ratioB = timeStep - ratioA

      Some(
        velocityNow.get * (ratioA * divisor) + velocityFuture.get * (ratioB * divisor)
      )
    } else {
      None
    }
  }

  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    hydrodynamicFlow.getVelocity(coordinate)
  }

  private def getInterpolatedVelocity(
      coordinate: GeoCoordinate,
      day: InterpolationTime
  ): Option[Velocity] = {
    val index = hydrodynamicFlow.getIndex(coordinate, day)
    val interpolation = new Interpolation()
    interpolation(coordinate, hydrodynamicFlow, index)
  }

  def updateHydrodynamicFlow(next: FlowGridWrapper): Unit = {
    hydrodynamicFlow = next
  }

  private def bumpCoordinate(coordinate: GeoCoordinate): GeoCoordinate = {
    val bumpedLongitude = coordinate.longitude + shiftAmount(
      RandomNumberGenerator.coinToss,
      Constants.MaxLongitudeShift
    )
    val bumpedLatitude = coordinate.latitude + shiftAmount(
      RandomNumberGenerator.coinToss,
      Constants.MaxLatitudeShift
    )
    new GeoCoordinate(bumpedLatitude, bumpedLongitude, coordinate.depth)
  }

  private def shiftAmount(toss: Boolean, maxShiftAmount: Double): Double =
    toss match {
      case true  => RandomNumberGenerator.get * maxShiftAmount
      case false => RandomNumberGenerator.get * maxShiftAmount * (-1)
    }

  private def correctNegativeCoordinate(value: Double): Double = {
    if (value < 0) then value + 180 else value
  }

  private def ensureDepthIsInRange(value: Int): Int = {
    if (value < 0) then 0
    else if (value > flow.dimensions.cellSize.depth) then
      flow.dimensions.cellSize.depth
    else value
  }
}

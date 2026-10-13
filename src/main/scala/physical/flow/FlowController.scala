package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.FlowFileIterator
import maths.interpolation.Interpolation
import physical.{GeoCoordinate, Velocity}

/**
  * The flow the larvae are carried by.
  *
  * The ocean model's daily fields are held two at a time, the pair whose times the clock
  * lies between, and the flow at a time is a blend of the two in proportion to how far
  * the time is from each.
  */
class FlowController(val reader: FlowFileIterator, var flow: Flow)
    extends Logging {

  private val interpolation = new Interpolation()
  private var hydrodynamicFlow = reader.current
  flow.dimensions = reader.flow.dimensions

  /** Moves on to the pair of daily fields that the time lies between. */
  def advanceTo(time: LocalDateTime): Unit =
    while (!time.isBefore(hydrodynamicFlow.laterStamp)) {
      hydrodynamicFlow = reader.next()
    }

  /** The flow at the nearest grid cell, from the earlier field only: enough to tell land from sea. */
  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    hydrodynamicFlow.getVelocity(coordinate)
  }

  /** The flow at the nearest grid cell at a time, blended between the two fields. */
  def getVelocity(coordinate: GeoCoordinate, time: LocalDateTime): Option[Velocity] = {
    val index = hydrodynamicFlow.getIndex(coordinate)
    blend(time)(isEarlier =>
      hydrodynamicFlow.getVelocity((index._1, index._2, index._3, isEarlier))
    )
  }

  /** The flow at a position and time: interpolated across the grid, and blended between the two fields. */
  def getVelocityOfCoordinate(
      coordinate: GeoCoordinate,
      time: LocalDateTime
  ): Option[Velocity] = {
    val index = hydrodynamicFlow.getIndex(coordinate)
    blend(time)(isEarlier =>
      interpolation(coordinate, hydrodynamicFlow, (index._1, index._2, index._3, isEarlier))
    )
  }

  // Only the field(s) the time needs are read; where either one is missing (land) so is the flow
  private def blend(time: LocalDateTime)(
      at: Boolean => Option[Velocity]
  ): Option[Velocity] = {
    val laterWeight = hydrodynamicFlow.weightOfLater(time)
    if (laterWeight <= 0.0) at(true)
    else if (laterWeight >= 1.0) at(false)
    else
      for {
        earlier <- at(true)
        later <- at(false)
      } yield earlier * (1.0 - laterWeight) + later * laterWeight
  }
}

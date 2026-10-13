package physical.flow

import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
import grizzled.slf4j.Logging
import io.FlowFileIterator
import maths.interpolation.Interpolation
import physical.{GeoCoordinate, Velocity}

/**
  * The flow the larvae are carried by.
  *
  * The ocean model's daily fields are held in pairs, and the flow at a time is a blend of
  * the pair whose times it lies between, in proportion to how far the time is from each.
  * The pair the clock lies in is held, and so is the next one whenever a time step runs on
  * past its later field: the end of a step, where it is sampled to move the larvae, can be
  * in the next pair.
  */
class FlowController(val reader: FlowFileIterator, var flow: Flow)
    extends Logging {

  private val interpolation = new Interpolation()
  // The pair the clock lies in first, then each one after it, a day on, that the step needs.
  // They are only changed in advanceTo, before the larvae are moved, never while they are.
  private var pairs: Vector[FlowGridWrapper] = Vector(reader.current)
  flow.dimensions = reader.flow.dimensions

  /**
    * Moves on to the pair of daily fields that the time lies between, and holds the pairs
    * up to a time step on as well.
    *
    * It fails with FlowDataExhaustedException when the files have nothing for the time
    * itself. Where they run out within the step, the last field is held for the rest of it,
    * as the first one is held until its own time.
    */
  def advanceTo(time: LocalDateTime): Unit = {
    while (!time.isBefore(pairs.head.laterStamp)) {
      if (pairs.size == 1) pairs :+= reader.next()
      pairs = pairs.tail
    }
    val endOfStep = time.plusSeconds(flow.timeStep.totalSeconds)
    try {
      while (endOfStep.isAfter(pairs.last.laterStamp)) pairs :+= reader.next()
    } catch {
      case _: FlowDataExhaustedException =>
    }
  }

  // The pair a time lies between: the first whose later field is after it, or, past them
  // all, the last
  private def pairAt(time: LocalDateTime): FlowGridWrapper =
    pairs.find(pair => time.isBefore(pair.laterStamp)).getOrElse(pairs.last)

  /** The flow at the nearest grid cell, from the earlier field only: enough to tell land from sea. */
  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    pairs.head.getVelocity(coordinate)
  }

  /** The flow at the nearest grid cell at a time, blended between the two fields. */
  def getVelocity(coordinate: GeoCoordinate, time: LocalDateTime): Option[Velocity] = {
    val pair = pairAt(time)
    val index = pair.getIndex(coordinate)
    blend(pair, time)(isEarlier =>
      pair.getVelocity((index._1, index._2, index._3, isEarlier))
    )
  }

  /** The flow at a position and time: interpolated across the grid, and blended between the two fields. */
  def getVelocityOfCoordinate(
      coordinate: GeoCoordinate,
      time: LocalDateTime
  ): Option[Velocity] = {
    val pair = pairAt(time)
    val index = pair.getIndex(coordinate)
    blend(pair, time)(isEarlier =>
      interpolation(coordinate, pair, (index._1, index._2, index._3, isEarlier))
    )
  }

  // Only the field(s) the time needs are read; where either one is missing (land) so is the flow
  private def blend(pair: FlowGridWrapper, time: LocalDateTime)(
      at: Boolean => Option[Velocity]
  ): Option[Velocity] = {
    val laterWeight = pair.weightOfLater(time)
    if (laterWeight <= 0.0) at(true)
    else if (laterWeight >= 1.0) at(false)
    else
      for {
        earlier <- at(true)
        later <- at(false)
      } yield earlier * (1.0 - laterWeight) + later * laterWeight
  }
}

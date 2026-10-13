package physical.flow

import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
import grizzled.slf4j.Logging
import io.FlowFileIterator
import physical.{GeoCoordinate, Velocity}

/**
  * The flow of one ocean model: its daily fields held in pairs, and the flow at a time as a blend
  * of the pair whose times it lies between, in proportion to how far the time is from each.
  * The pair the clock lies in is held, and so is the next one whenever a time step runs on
  * past its later field: the end of a step, where it is sampled to move the larvae, can be
  * in the next pair.
  */
class FlowLayer(val reader: FlowFileIterator) extends Logging {

  // The pair the clock lies in first, then each one after it, a day on, that the step needs.
  // They are only changed in advanceTo, before the larvae are moved, never while they are.
  private var pairs: Vector[FlowGridWrapper] = Vector(reader.current)

  /**
    * Moves on to the pair of daily fields that the time lies between, and holds the pairs
    * up to a time step on as well.
    *
    * It fails with FlowDataExhaustedException when the files have nothing for the time
    * itself. Where they run out within the step, the last field is held for the rest of it,
    * as the first one is held until its own time.
    */
  def advanceTo(time: LocalDateTime, stepSeconds: Int): Unit = {
    while (!time.isBefore(pairs.head.laterStamp)) {
      if (pairs.size == 1) pairs :+= reader.next()
      pairs = pairs.tail
    }
    val endOfStep = time.plusSeconds(stepSeconds)
    try {
      while (endOfStep.isAfter(pairs.last.laterStamp)) pairs :+= reader.next()
    } catch {
      case _: FlowDataExhaustedException =>
    }
  }

  /** Whether the position is on this model's grid, which a nested model's is only a part of. */
  def covers(coordinate: GeoCoordinate): Boolean = pairs.head.covers(coordinate)

  // The pair a time lies between: the first whose later field is after it, or, past them
  // all, the last
  private def pairAt(time: LocalDateTime): FlowGridWrapper =
    pairs.find(pair => time.isBefore(pair.laterStamp)).getOrElse(pairs.last)

  /** The flow at the nearest grid cell, from the earlier field only: enough to tell land from sea. */
  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] =
    pairs.head.getVelocity(coordinate)

  /** The flow at the nearest grid cell at a time, blended between the two fields. */
  def getVelocity(coordinate: GeoCoordinate, time: LocalDateTime): Option[Velocity] = {
    val pair = pairAt(time)
    pair.getIndex(coordinate) // fails if the position is off the grid
    blend(pair, time)(isEarlier => pair.nearestVelocity(coordinate, isEarlier))
  }

  /** The flow at a position and time: interpolated across the grid, and blended between the two fields. */
  def getVelocityOfCoordinate(coordinate: GeoCoordinate, time: LocalDateTime): Option[Velocity] = {
    val pair = pairAt(time)
    pair.getIndex(coordinate) // fails if the position is off the grid
    pair.interpolatedVelocity(coordinate, pair.weightOfLater(time))
  }

  /** The temperature in degrees C at a position and time, interpolated and blended like the flow; None where there is none or it was not read. */
  def getTemperature(coordinate: GeoCoordinate, time: LocalDateTime): Option[Double] = {
    val pair = pairAt(time)
    pair.temperature(coordinate, pair.weightOfLater(time))
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

/**
  * The flow the larvae are carried by: an ocean model's, and any finer models nested inside it.
  *
  * A position is given the flow of the first nested model whose grid it lies on, and otherwise
  * the main model's. Inside a nested model the coast is where that model puts it, so a position
  * it has no flow at (land) has none, even where the main model has sea there. A nested model
  * whose files end before the run does is dropped with a warning from then on, and the main
  * model carries the larvae through the area.
  */
class FlowController(val reader: FlowFileIterator, var flow: Flow, nests: Seq[FlowFileIterator] = Seq.empty)
    extends Logging {

  private val main = new FlowLayer(reader)
  // the nested models, in the order they are tried; finest first
  private var nested: Vector[FlowLayer] = nests.map(new FlowLayer(_)).toVector
  flow.dimensions = reader.flow.dimensions

  /**
    * Moves every model on to the pair of daily fields that the time lies between. It fails with
    * FlowDataExhaustedException when the main model's files have nothing for the time; a nested
    * model's running out is only warned of.
    */
  def advanceTo(time: LocalDateTime): Unit = {
    val seconds = flow.timeStep.totalSeconds
    main.advanceTo(time, seconds)
    nested = nested.filter { layer =>
      try {
        layer.advanceTo(time, seconds)
        true
      } catch {
        case e: FlowDataExhaustedException =>
          warn(s"${e.getMessage} The nested flow in ${layer.reader.flow.netcdfFilePath} ends at $time; the main flow is used there from now on.")
          layer.reader.shutdown()
          false
      }
    }
  }

  // The model a position is in: the first nested one with it on its grid, else the main one,
  // which fails for a position off its grid
  private def layerAt(coordinate: GeoCoordinate): FlowLayer =
    nested.find(_.covers(coordinate)).getOrElse(main)

  /** The flow at the nearest grid cell, from the earlier field only: enough to tell land from sea. */
  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] =
    layerAt(coordinate).getVelocity(coordinate)

  /** The flow at the nearest grid cell at a time, blended between the two fields. */
  def getVelocity(coordinate: GeoCoordinate, time: LocalDateTime): Option[Velocity] =
    layerAt(coordinate).getVelocity(coordinate, time)

  /** The flow at a position and time: interpolated across the grid, and blended between the two fields. */
  def getVelocityOfCoordinate(coordinate: GeoCoordinate, time: LocalDateTime): Option[Velocity] =
    layerAt(coordinate).getVelocityOfCoordinate(coordinate, time)

  /** The temperature in degrees C at a position and time, from the main model's `temp` files (none where there are none). */
  def getTemperature(coordinate: GeoCoordinate, time: LocalDateTime): Option[Double] =
    main.getTemperature(coordinate, time)
}

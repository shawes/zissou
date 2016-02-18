package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.FlowReader
import locals.Constants
import maths.RandomNumberGenerator
import maths.interpolation.Interpolator
import physical.{GeoCoordinate, Velocity}
import utilities.Timer

import scala.collection.mutable


class FlowController(var flow: Flow, val randomNumbers: RandomNumberGenerator) extends Logging {

  val SizeOfQueue = 2
  val flowDataQueue = mutable.Queue.empty[Array[FlowPolygon]]
  val interpolator = new Interpolator(flow.dimensions)


  def getVelocityOfCoordinate(coordinate: GeoCoordinate, future: DateTime, now: DateTime, timeStep: Int): Velocity = {
    require(future >= now, "flow field information for this period is not loaded into memory")

    val currentVelocity = getVelocityOfCoordinate(coordinate, isFuture = false)
    val futureVelocity = getVelocityOfCoordinate(coordinate, isFuture = true)

    if (future == now) return currentVelocity
    if (future == now.plusSeconds(timeStep)) return futureVelocity
    if (future > now && future < now.plusSeconds(timeStep)) {
      val period: Duration = new Duration(future, now)
      val divisor = 1 / timeStep.toDouble
      val ratioA = period.toStandardSeconds.getSeconds
      val ratioB = timeStep - ratioA
      return currentVelocity * (ratioA * divisor) + futureVelocity * (ratioB * divisor)
    }
    new Velocity(Double.NaN, Double.NaN)
  }

  def getVelocityOfCoordinate(coordinate: GeoCoordinate, isFuture: Boolean): Velocity = {

    var velocity: Velocity = new Velocity(Double.NaN, Double.NaN)

    val flowPolygons: Array[FlowPolygon] = if (isFuture) flowDataQueue.last else flowDataQueue.head
    var index = getIndexOfPolygon(coordinate)
    val velocityAtCentroid = flowPolygons(index).velocity
    if (!velocityAtCentroid.isUndefined) {
      trace("Index of flowpolygon is: " + index + ", with coord " + coordinate + ", and centroid velocity is " + velocityAtCentroid)

      if (index == Constants.LightWeightException.CoordinateNotFoundException) {
        val bumpedCoordinate = bumpCoordinate(coordinate)
        index = getIndexOfPolygon(bumpedCoordinate)
        if (index != Constants.LightWeightException.CoordinateNotFoundException) {
          velocity = interpolator.interpolate(bumpedCoordinate, flowPolygons, index)
        }
      } else {
        velocity = interpolator.interpolate(coordinate, flowPolygons, index)
      }
      trace("The interpolated velocity is " + velocity)
      if (velocity.isUndefined) velocityAtCentroid else velocity
    }
    velocity
  }

  def getIndexOfPolygon(coordinate: GeoCoordinate): Int = {
    if (flow.dimensions.latitudeBoundary.contains(coordinate.latitude) && flow.dimensions.longitudeBoundary.contains(coordinate.longitude)) {

      val lat1 = correctNegativeCoordinate(coordinate.latitude)
      val lon1 = correctNegativeCoordinate(coordinate.longitude)
      val lat2 = correctNegativeCoordinate(flow.dimensions.latitudeBoundary.start)
      val lon2 = correctNegativeCoordinate(flow.dimensions.longitudeBoundary.start)

      val x = ((lat1 - lat2) / flow.dimensions.cellSize.cell.width).toInt + 1
      val y = ((lon1 - lon2) / flow.dimensions.cellSize.cell.width).toInt + 1
      val z = ensureDepthIsInRange((coordinate.depth / flow.dimensions.cellSize.cell.depth).toInt)

      flow.dimensions.cellSize.width * x + y + z * flow.dimensions.cellSize.layerCellCount
    } else {
      println("coodinate not found is: " + coordinate.toString)
      Constants.LightWeightException.CoordinateNotFoundException
    }
  }

  def correctNegativeCoordinate(value: Double): Double = {
    if (value < 0) value + 180 else value
  }

  def ensureDepthIsInRange(value: Int): Int = {
    if (value < 0) 0 else if (value > flow.dimensions.cellSize.depth) flow.dimensions.cellSize.depth else value
  }

  def bumpCoordinate(coordinate: GeoCoordinate): GeoCoordinate = {
    val longitude = if (randomNumbers.coinToss) coordinate.longitude + Constants.MaxLongitudeShift else coordinate.longitude - Constants.MaxLongitudeShift
    val latitude = if (randomNumbers.coinToss) coordinate.latitude + Constants.MaxLatitudeShift else coordinate.latitude - Constants.MaxLatitudeShift
    new GeoCoordinate(latitude, longitude, coordinate.depth)
  }

  def refresh(polygons: Array[FlowPolygon]) {
    if (flowDataQueue.nonEmpty) {
      flowDataQueue.dequeue()
    }
    flowDataQueue += polygons
  }

  def initialiseFlow(reader: FlowReader) {
    for (i <- 0 until SizeOfQueue) {
      val timer = new Timer()
      if (reader.hasNext) flowDataQueue += reader.next()
      debug("Finished reading the next flow data in " + timer.stop() + " seconds")
    }
    flow.dimensions = reader.flow.dimensions
    interpolator.dim = reader.flow.dimensions
  }


}
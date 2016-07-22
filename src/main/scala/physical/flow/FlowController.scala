package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.FlowFile
import locals.Constants
import maths.RandomNumberGenerator
import maths.interpolation.Interpolation
import physical.{GeoCoordinate, Velocity}
import utilities.Timer

import scala.collection.mutable

//mport scala.collection.mutable.Stack
//import scala.collection.parallel.immutable


class FlowController(var flow: Flow) extends Logging {

  //val SizeOfQueue = 2
  //val flowDataQueue = mutable.Queue.empty[FlowGridWrapper]
  val interpolation = new Interpolation()
  var flowGrids = mutable.Stack[FlowGridWrapper]()


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


    var index = flowGrids.head.getIndex(coordinate)
    if (isFuture) {
      index(3) = 1
    }

    val velocityAtCentroid = flowGrids.head.getVelocity(coordinate)
    if (velocityAtCentroid.isDefined) {
      trace("Index of flowpolygon is: " + index + ", with coord " + coordinate + ", and centroid velocity is " + velocityAtCentroid)

      if (index(0) == Constants.LightWeightException.CoordinateNotFoundException) {
        val bumpedCoordinate = bumpCoordinate(coordinate)
        index = flowGrids.head.getIndex(bumpedCoordinate)
        if (index(0) != Constants.LightWeightException.CoordinateNotFoundException) {
          velocity = interpolation(bumpedCoordinate, flowGrids.head, index)
        }
      } else {
        velocity = interpolation(coordinate, flowGrids.head, index)
      }
      trace("The interpolated velocity is " + velocity)
      if (velocity.isUndefined) velocityAtCentroid else velocity
    }
    velocity
  }

  def bumpCoordinate(coordinate: GeoCoordinate): GeoCoordinate = {
    val bumpedLongitude = coordinate.longitude + shiftAmount(RandomNumberGenerator.coinToss, Constants.MaxLongitudeShift)
    val bumpedLatitude = coordinate.latitude + shiftAmount(RandomNumberGenerator.coinToss, Constants.MaxLatitudeShift)
    new GeoCoordinate(bumpedLatitude, bumpedLongitude, coordinate.depth)
  }

  private def shiftAmount(toss: Boolean, maxShiftAmount: Double): Double = toss match {
    case true => RandomNumberGenerator.get * maxShiftAmount
    case false => RandomNumberGenerator.get * maxShiftAmount * (-1)
  }

  def initialise(reader: FlowFile) {
    // for (i <- 0 until SizeOfQueue) {
    val timer = new Timer()
    if (reader.hasNext) refresh(reader.next())
    debug("Finished reading the next flow data in " + timer.stop() + " seconds")
    //}
    flow.dimensions = reader.flow.dimensions
  }

  def refresh(newGrid: FlowGridWrapper) {
    flowGrids.clear()
    flowGrids.push(newGrid)
  }

  private def correctNegativeCoordinate(value: Double): Double = {
    if (value < 0) value + 180 else value
  }

  private def ensureDepthIsInRange(value: Int): Int = {
    if (value < 0) 0 else if (value > flow.dimensions.cellSize.depth) flow.dimensions.cellSize.depth else value
  }


}
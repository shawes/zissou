package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.FlowReader
import locals.Constants
import maths.RandomNumberGenerator
import maths.interpolation.Interpolator
import physical.{GeoCoordinate, Velocity}

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
    new Velocity
  }

  def getVelocityOfCoordinate(coordinate: GeoCoordinate, isFuture: Boolean): Velocity = {

    var index: Int = 0
    var bumped: Boolean = false
    var bumpedCoordinate: GeoCoordinate = new GeoCoordinate()
    //var coord = coordinate

    val flowPolygons: Array[FlowPolygon] = if (isFuture) flowDataQueue.last else flowDataQueue.head
    //logger.debug("Flow polygons is size " + flowPolygons.length)
    //logger.debug("Coordinate is " + coordinate)

    try {
      index = getIndexOfPolygon(coordinate)
      debug("Found the index " + index)
      //logger.debug("flow lr")
      //val polygon = flowPolygons(index)
    } catch {
      case e: IllegalArgumentException =>
        warn("Have to bump the coordinate " + coordinate)
        bumpedCoordinate = bumpCoordinate(coordinate)

        try {
          index = getIndexOfPolygon(bumpedCoordinate)
          bumped = true
        } catch {
          case e: IllegalArgumentException =>
            error("The coordinate " + bumpedCoordinate + " is not in the flow field")
            return new Velocity()
        }
    }
    debug("The velocity is " + flowPolygons(index).velocity)
    if (!flowPolygons(index).velocity.isUndefined) {
      if (bumped) {
        interpolator.interpolate(bumpedCoordinate, flowPolygons, index)
      } else {
        interpolator.interpolate(coordinate, flowPolygons, index)
      }
    } else {
      new Velocity()
    }

  }

  def getIndexOfPolygon(coordinate: GeoCoordinate): Int = {
    debug("The flow: " + flow.dimensions.latitudeBoundary)
    require(flow.dimensions.latitudeBoundary.contains(coordinate.latitude) && flow.dimensions.longitudeBoundary.contains(coordinate.longitude))

    val lat1 = correctNegativeCoordinate(coordinate.latitude)
    val lon1 = correctNegativeCoordinate(coordinate.longitude)
    val lat2 = correctNegativeCoordinate(flow.dimensions.latitudeBoundary.start)
    val lon2 = correctNegativeCoordinate(flow.dimensions.longitudeBoundary.start)

    val x = ((lat1 - lat2) / flow.dimensions.cellSize.cell.width).toInt + 1
    val y = ((lon1 - lon2) / flow.dimensions.cellSize.cell.width).toInt + 1
    val z = ensureDepthIsInRange((coordinate.depth / flow.dimensions.cellSize.cell.depth).toInt)

    flow.dimensions.cellSize.width * x + y + z * flow.dimensions.cellSize.layerCellCount
  }

  def correctNegativeCoordinate(value: Double): Double = {
    if (value < 0) value + 180 else value
  }

  def ensureDepthIsInRange(value: Int): Int = {
    if (value < 0) 0 else if (value > flow.dimensions.cellSize.depth) flow.dimensions.cellSize.depth else value
  }

  def bumpCoordinate(coordinate: GeoCoordinate): GeoCoordinate = {
    new GeoCoordinate(coordinate.latitude + (randomNumbers.get * Constants.MaxLatitudeShift),
      coordinate.longitude + (randomNumbers.get * Constants.MaxLongitudeShift), coordinate.depth)
  }

  def refresh(polygons: Array[FlowPolygon]) {
    debug("Refreshing the queue")
    if (flowDataQueue.nonEmpty) {
      debug("Queue size is " + flowDataQueue.size + "before dequeuing")
      flowDataQueue.dequeue()
      debug("Queue size is " + flowDataQueue.size + "and after dequeuing")
    }
    flowDataQueue += polygons
    debug("Queue size is " + flowDataQueue.size + "and after enqueuing")
  }

  def initialiseFlow(reader: FlowReader) {
    for (i <- 0 until SizeOfQueue) {
      debug("Reading the first flow data")
      debug("started reading flow queue")
      val start = DateTime.now
      if (reader.hasNext) flowDataQueue += reader.next()
      val seconds = DateTime.now.getSecondOfDay - start.getSecondOfDay
      debug("finished reading in " + seconds + " seconds")
    }
    flow.dimensions = reader.flow.dimensions
    interpolator.dim = reader.flow.dimensions
  }

  //  override def finalize(): Unit = {
  //     flowDataQueue.dequeueAll(x => true)
  //  }


}
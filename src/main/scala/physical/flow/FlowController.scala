package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j._
import io.FlowReader
import locals.Constants
import maths.RandomNumberGenerator
import maths.interpolation.Interpolator
import physical.{GeoCoordinate, Velocity}

import scala.collection.mutable


class FlowController(var flow: Flow, val randomNumbers: RandomNumberGenerator) {

  val SizeOfQueue = 2
  val flowDataQueue = mutable.Queue.empty[Array[FlowPolygon]]
  val logger = Logger(classOf[FlowController])
  val interpolator = new Interpolator(flow)


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

    val flowPolygons: Array[FlowPolygon] = if (isFuture) flowDataQueue.last else flowDataQueue.head
    //logger.debug("Flow polygons is size " + flowPolygons.length)
    //logger.debug("Coordinate is " + coordinate)

    try {
      index = getIndexOfPolygon(coordinate)
      logger.debug("Found the index " + index)
      //logger.debug("flow lr")
      //val polygon = flowPolygons(index)
    } catch {
      case e: IllegalArgumentException =>
        logger.warn("Have to bump the coordinate " + coordinate)
        val bumpedCoordinate = bumpCoordinate(coordinate)

        try {
          index = getIndexOfPolygon(bumpedCoordinate)
        } catch {
          case e: IllegalArgumentException =>
            logger.error("The coordinate " + bumpedCoordinate + " is not in the flow field")
            return new Velocity()
        }
    }
    logger.debug("The velocity is " + flowPolygons(index).velocity)
    //val velocity = flowPolygons(index).velocity
    interpolator.interpolate(coordinate, flowPolygons, index)

  }

  def getIndexOfPolygon(coordinate: GeoCoordinate): Int = {
    require(flow.latitudeRange.contains(coordinate.latitude) && flow.longitudeRange.contains(coordinate.longitude))

    val lat1 = correctNegativeCoordinate(coordinate.latitude)
    val lon1 = correctNegativeCoordinate(coordinate.longitude)
    val lat2 = correctNegativeCoordinate(flow.latitudeRange.start)
    val lon2 = correctNegativeCoordinate(flow.longitudeRange.start)

    val x = ((lat1 - lat2) / flow.grid.cell.width).toInt + 1
    val y = ((lon1 - lon2) / flow.grid.cell.width).toInt + 1
    val z = ensureDepthIsInRange((coordinate.depth / flow.grid.cell.depth).toInt)

    flow.grid.width * x + y + z * flow.grid.layerCellCount
  }

  def correctNegativeCoordinate(value: Double): Double = {
    if (value < 0) value + 180 else value
  }

  def ensureDepthIsInRange(value: Int): Int = {
    if (value < 0) 0 else if (value > flow.grid.depth) flow.grid.depth else value
  }

  def bumpCoordinate(coordinate: GeoCoordinate): GeoCoordinate = {
    //val random = new MersenneTwister(Platform.currentTime.toInt)
    new GeoCoordinate(coordinate.latitude + (randomNumbers.get * Constants.MaxLatitudeShift),
      coordinate.longitude + (randomNumbers.get * Constants.MaxLongitudeShift), coordinate.depth)
  }

  def refresh(polygons: Array[FlowPolygon]) {
    logger.debug("Refreshing the queue")
    if (flowDataQueue.nonEmpty) {
      logger.debug("Queue size is " + flowDataQueue.size + "before dequeuing")
      flowDataQueue.dequeue()
      logger.debug("Queue size is " + flowDataQueue.size + "and after dequeuing")
    }
    flowDataQueue += polygons
    logger.debug("Queue size is " + flowDataQueue.size + "and after enqueuing")
  }

  def initialiseFlow(reader: FlowReader) {
    for (i <- 0 until SizeOfQueue) {
      logger.debug("Reading the first flow data")
      System.out.println("started reading flow queue")
      val start = DateTime.now
      if (reader.hasNext) flowDataQueue += reader.next()
      val seconds = DateTime.now.getSecondOfDay - start.getSecondOfDay
      System.out.println("finished reading in " + seconds + " seconds")
    }
    flow = reader.flow
    interpolator.flow = flow
  }

  //  override def finalize(): Unit = {
  //     flowDataQueue.dequeueAll(x => true)
  //  }


}
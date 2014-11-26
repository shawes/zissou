package physical.flow

import com.github.nscala_time.time.Imports._
import io.{FlowReader, Logger}
import locals.Constants
import org.apache.commons.math.random.MersenneTwister
import physical.{GeoCoordinate, Velocity}

import scala.collection.mutable
import scala.compat.Platform

class FlowController(var flow: Flow) {

  val SizeOfQueue = 2
  val flowDataQueue: mutable.Queue[Array[FlowPolygon]] = new mutable.Queue[Array[FlowPolygon]]


  def getVelocityOfCoordinate(coordinate: GeoCoordinate, future: DateTime, now: DateTime, timeStep: Int): Velocity = {
    require(future > now, "flow field information for this period is not loaded into memory")

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

    try {
      index = getIndexOfPolygon(coordinate)
      //val polygon = flowPolygons(index)
    } catch {
      case e: IllegalArgumentException =>
        val bumpedCoordinate = bumpCoordinate(coordinate)
        index = getIndexOfPolygon(bumpedCoordinate)
    }
    flowPolygons(index).velocity


  }

  def getIndexOfPolygon(coordinate: GeoCoordinate): Int = {
    require(flow.latitudeRange.contains(coordinate.latitude) && flow.latitudeRange.contains(coordinate.longitude))

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
    val random = new MersenneTwister(Platform.currentTime.toInt)
    new GeoCoordinate(coordinate.latitude + (random.nextDouble() * Constants.MaxLatitudeShift),
      coordinate.longitude + (random.nextDouble() * Constants.MaxLongitudeShift))
  }

  def refresh(polygons: Array[FlowPolygon]) {
    if (flowDataQueue.size > 0) flowDataQueue.dequeue()
    flowDataQueue.enqueue(polygons)
  }

  def initialiseFlow(reader: FlowReader) {
    for (i <- 0 until SizeOfQueue) {
      Logger.info("Reading the first flow data")
      if (reader.hasNext) flowDataQueue.enqueue(reader.next())
    }
    flow = reader.flow
  }


}
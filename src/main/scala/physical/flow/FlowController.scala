package physical.flow

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import io.FlowFileIterator
import locals.Constants
import locals.Constants.NetcdfIndex
import locals.Day._
import maths.RandomNumberGenerator
import maths.interpolation.Interpolation
import org.joda.time.Duration
import physical.{GeoCoordinate, Velocity}
import utilities.SimpleTimer

import scala.collection.mutable

//mport scala.collection.mutable.Stack
//import scala.collection.parallel.immutable


class FlowController(var flow: Flow) extends Logging {

  //val SizeOfQueue = 2
  //val flowDataQueue = mutable.Queue.empty[FlowGridWrapper]
  val interpolation = new Interpolation()
  var flowGrids = mutable.Stack[FlowGridWrapper]()

  def getVelocityOfCoordinate(coordinate: GeoCoordinate, future: DateTime, now: DateTime, timeStep: Int): Option[Velocity] = {
    require(future >= now && future <= now.plusSeconds(timeStep), "Time step is not loaded into memory")

    if (future == now) {
      getVelocityOfCoordinate(coordinate, Today)
    } else if (future == now.plusSeconds(timeStep)) {
      getVelocityOfCoordinate(coordinate, Tomorrow)
    } else {
      derivePartialTimeStepVelocity(coordinate, future, now, timeStep)
    }
  }

  private def derivePartialTimeStepVelocity(coordinate: GeoCoordinate, future: DateTime, now: DateTime, timeStep: Int): Option[Velocity] = {
    val velocityNow = getVelocityOfCoordinate(coordinate, Today)
    val velocityFuture = getVelocityOfCoordinate(coordinate, Tomorrow)

    if (velocityNow.isDefined && velocityFuture.isDefined) {
      val divisor = 1 / timeStep.toDouble
      val period: Duration = new Duration(future, now)

      val ratioA = period.toStandardSeconds.getSeconds
      val ratioB = timeStep - ratioA

      Some(velocityNow.get * (ratioA * divisor) + velocityFuture.get * (ratioB * divisor))
    }
    else {
      None
    }

  }


  def getVelocityOfCoordinate(coordinate: GeoCoordinate, day: Day): Option[Velocity] = {
    val index = flowGrids.head.getIndex(coordinate, day)
    debug("The index of the flowGrid is: "+ index)
    // Move the particle upwards if there is no velocity found at the depth (assuming its not that deep)
    // while (!flowGrids.head.getVelocity(index).isDefined && index(NetcdfIndex.Z) > 0) {
    //   index(NetcdfIndex.Z) -= 1
    // }
    debug("The velocity of the flowGrid is: "+ flowGrids.head.getVelocity(index))
    interpolation(coordinate, flowGrids.head, index)
  }

  def initialise(reader: FlowFileIterator) {
    val timer = new SimpleTimer()
    timer.start()
    if (reader.hasNext) refresh(reader.next())
    timer.stop()
    debug("Finished reading the next flow data in " + timer.result() + " seconds")
    flow.dimensions = reader.flow.dimensions
  }

  def refresh(newGrid: FlowGridWrapper) {
    flowGrids.clear()
    flowGrids.push(newGrid)
  }

  private def bumpCoordinate(coordinate: GeoCoordinate): GeoCoordinate = {
    val bumpedLongitude = coordinate.longitude + shiftAmount(RandomNumberGenerator.coinToss, Constants.MaxLongitudeShift)
    val bumpedLatitude = coordinate.latitude + shiftAmount(RandomNumberGenerator.coinToss, Constants.MaxLatitudeShift)
    new GeoCoordinate(bumpedLatitude, bumpedLongitude, coordinate.depth)
  }

  private def shiftAmount(toss: Boolean, maxShiftAmount: Double): Double = toss match {
    case true => RandomNumberGenerator.get * maxShiftAmount
    case false => RandomNumberGenerator.get * maxShiftAmount * (-1)
  }

  private def correctNegativeCoordinate(value: Double): Double = {
    if (value < 0) value + 180 else value
  }

  private def ensureDepthIsInRange(value: Int): Int = {
    if (value < 0) 0 else if (value > flow.dimensions.cellSize.depth) flow.dimensions.cellSize.depth else value
  }


}

package physical.flow

import com.github.nscala_time.time.Imports.{DateTimeZone, LocalDateTime}
import exceptions.{CoordinateOutOfBoundsException, InterpolationNotImplementedException}
import grizzled.slf4j.Logging
import locals.Constants.NetcdfIndex
import locals._
import physical.adaptors.LatLonPointToGeoCoordinateAdaptor
import physical.{GeoCoordinate, Velocity}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid
import scala.collection.parallel.mutable._

/**
  * Two daily flow fields, the earlier first, and the times they are for.
  *
  * @param data the u, v and w grids of the earlier field, then those of the later
  * @param earlierStamp the time the earlier field is for, from the time axis of the data
  * @param laterStamp the time the later field is for
  */
class FlowGridWrapper(
    val depths: List[Double],
    val data: List[List[(Array[Array[Array[Float]]], GridCoordSystem)]],
    val earlierStamp: LocalDateTime,
    val laterStamp: LocalDateTime
) extends Logging {

  private lazy val earlierMillis = earlierStamp.toDateTime(DateTimeZone.UTC).getMillis
  private lazy val laterMillis = laterStamp.toDateTime(DateTimeZone.UTC).getMillis

  /**
    * How far a time lies between the two fields: 0 at or before the earlier one's time,
    * 1 at or after the later one's, and in proportion in between, so that the flow can
    * be blended in time instead of jumping from one day's field to the next.
    */
  def weightOfLater(time: LocalDateTime): Double = {
    val span = (laterMillis - earlierMillis).toDouble
    if (span <= 0) 0.0
    else {
      val elapsed = (time.toDateTime(DateTimeZone.UTC).getMillis - earlierMillis).toDouble
      math.min(1.0, math.max(0.0, elapsed / span))
    }
  }

  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    val gridIndex = getIndex(coordinate)
    getVelocity(gridIndex)
  }

  // The u, v and w grids for today and tomorrow, read straight from the arrays
  private lazy val today = data.head.map(_._1).toArray
  private lazy val tomorrow = data.last.map(_._1).toArray
  private lazy val depthArray = depths.toArray

  def getVelocity(index: (Int, Int, Int, Boolean)): Option[Velocity] =
    velocityAt(index._1, index._2, index._3, index._4)

  private def velocityAt(
      x: Int,
      y: Int,
      z: Int,
      isToday: Boolean
  ): Option[Velocity] = {
    val components = if (isToday) today else tomorrow
    val velocity = new Velocity(
      components(0)(z)(y)(x).toDouble,
      components(1)(z)(y)(x).toDouble,
      components(2)(z)(y)(x).toDouble
    )
    if (velocity.isDefined) Some(velocity) else None
  }

  // The nearest depth level; a tie goes to the shallower level
  private def closestDepthIndex(depth: Double): Int = {
    var closest = 0
    var closestDistance = math.abs(depthArray(0) - depth)
    var i = 1
    while (i < depthArray.length) {
      val distance = math.abs(depthArray(i) - depth)
      if (distance < closestDistance) {
        closest = i
        closestDistance = distance
      }
      i += 1
    }
    closest
  }

  // The longitude of each column of nodes and the latitude of each row, read from the
  // data's own coordinate axes, in whichever direction they run
  private lazy val longitudes: Array[Double] =
    Array.tabulate(data.head.head._1(0)(0).length)(x => data.head.head._2.getLatLon(x, 0).getLongitude)
  private lazy val latitudes: Array[Double] =
    Array.tabulate(data.head.head._1(0).length)(y => data.head.head._2.getLatLon(0, y).getLatitude)

  /** The flow at the grid nodes round a position, and where the position is in the gap between the middle ones. */
  final class Stencil(
      val nodes: Array[Array[Velocity]], // indexed [longitude][latitude]
      val longitudeFraction: Double, // 0 to 1, from the first middle node to the second
      val latitudeFraction: Double
  )

  /**
    * The flow at the `size` by `size` block of grid nodes with the position inside its middle
    * gap, for interpolating it. That is not always the block centred on the nearest node: the
    * position lies between that node and the next one, on whichever side it is. Nothing is
    * returned where the block runs off the grid or takes in a node with no flow (land).
    */
  def stencil(
      coordinate: GeoCoordinate,
      interpolation: InterpolationType,
      index: (Int, Int, Int, Boolean)
  ): Option[Stencil] = {
    val size = neighbourhoodSize(interpolation)
    val before = size / 2 - 1 // nodes before the gap
    val lonNode = gapStart(longitudes, index._1, coordinate.longitude)
    val latNode = gapStart(latitudes, index._2, coordinate.latitude)
    if (lonNode < 0 || latNode < 0) None
    else {
      val nodes = nodesFrom(lonNode - before, latNode - before, size, index._3, index._4)
      if (nodes == null) None
      else
        Some(
          new Stencil(
            nodes,
            fractionAcross(longitudes, lonNode, coordinate.longitude),
            fractionAcross(latitudes, latNode, coordinate.latitude)
          )
        )
    }
  }

  // The node k with the value between nodes k and k+1, or -1 if there is none, given the nearest
  // node `cell`: the value is on one side of it, so in the gap above or the gap below. Signed
  // differences make it the same for axes that run either way.
  private def gapStart(nodes: Array[Double], cell: Int, value: Double): Int =
    if (isInGap(nodes, cell, value)) cell
    else if (isInGap(nodes, cell - 1, value)) cell - 1
    else -1

  private def isInGap(nodes: Array[Double], k: Int, value: Double): Boolean =
    k >= 0 && k + 1 < nodes.length && {
      val fraction = fractionAcross(nodes, k, value)
      fraction >= 0.0 && fraction <= 1.0
    }

  private def fractionAcross(nodes: Array[Double], k: Int, value: Double): Double =
    (value - nodes(k)) / (nodes(k + 1) - nodes(k))

  // The flow at a block of nodes, indexed by [longitude][latitude]; null if any is off the grid or land
  private def nodesFrom(
      firstX: Int,
      firstY: Int,
      size: Int,
      z: Int,
      isEarlier: Boolean
  ): Array[Array[Velocity]] = {
    if (firstX < 0 || firstY < 0 || firstX + size > longitudes.length || firstY + size > latitudes.length) null
    else {
      // allocated directly: Array.ofDim looks up a class tag every time, which was a sixth of the run
      val nodes = new Array[Array[Velocity]](size)
      var i = 0
      while (i < size) {
        nodes(i) = new Array[Velocity](size)
        var j = 0
        while (j < size) {
          velocityAt(firstX + i, firstY + j, z, isEarlier) match {
            case Some(velocity) => nodes(i)(j) = velocity
            case None           => return null
          }
          j += 1
        }
        i += 1
      }
      nodes
    }
  }

  def getIndex(coordinate: GeoCoordinate): (Int, Int, Int, Boolean) = {
    getIndex(coordinate, InterpolateToday)
  }

  def getIndex(
      coordinate: GeoCoordinate,
      day: InterpolationTime
  ): (Int, Int, Int, Boolean) = {
    val indexXY = data.head.head._2
      .findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    if (!isInsideGrid(indexXY(0), indexXY(1))) {
      throw new CoordinateOutOfBoundsException(
        s"(${coordinate.latitude}, ${coordinate.longitude}) is outside the flow grid"
      )
    }
    val indexZ = closestDepthIndex(coordinate.depth)
    val indexT = timeIndex(day)
    // (x, y, z ,t)
    (indexXY(0), indexXY(1), indexZ, indexT)
  }

  // The index lookup answers -1 for a point outside the grid
  private def isInsideGrid(x: Int, y: Int): Boolean = {
    val surface = data.head.head._1(0)
    x >= 0 && y >= 0 && y < surface.length && x < surface(0).length
  }

  private def timeIndex(day: InterpolationTime): Boolean = day match {
    case InterpolateToday    => true
    case InterpolateTomorrow => false
  }

  private def neighbourhoodSize(interpolation: InterpolationType): Int =
    interpolation match {
      case Bilinear =>
        math.sqrt(Constants.Interpolation.CubicPoints).toInt
      case Bicubic =>
        math.sqrt(Constants.Interpolation.BicubicPoints).toInt
      case Tricubic =>
        math.sqrt(Constants.Interpolation.TricubicPoints).toInt
      case _ => throw new InterpolationNotImplementedException()
    }
}

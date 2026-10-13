package physical.flow

import com.github.nscala_time.time.Imports.{DateTimeZone, LocalDateTime}
import exceptions.CoordinateOutOfBoundsException
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
  * @param depths the depth of each level of the u grid, from the surface
 * @param componentDepths the depths of the levels of u, v and w each, when they differ (BRAN2020 has
 *                        w at the interfaces between the levels of u and v); `depths` for all if empty
 * @param data the u, v and w grids of the earlier field, then those of the later
  * @param earlierStamp the time the earlier field is for, from the time axis of the data
  * @param laterStamp the time the later field is for
  */
class FlowGridWrapper(
    val depths: List[Double],
    val data: List[List[(Array[Array[Array[Float]]], GridCoordSystem)]],
    val earlierStamp: LocalDateTime,
    val laterStamp: LocalDateTime,
    val componentDepths: List[List[Double]] = List.empty
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

  /** The flow at the nearest node of the earlier field, enough to tell land from sea. */
  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    getIndex(coordinate) // fails if the position is off the grid
    nearestVelocity(coordinate, true)
  }

  /**
    * The flow at the nearest node, of the earlier or the later field. Where u, v and w are on one
    * grid that is the node of `index`; where they are not, each is read from the nearest node of its
    * own grid and the nearest level of its own depths.
    */
  def nearestVelocity(coordinate: GeoCoordinate, isEarlier: Boolean): Option[Velocity] = {
    val where = locate(coordinate, withGeometry = false)
    val velocity = new Velocity(nearestValue(0, where, isEarlier), nearestValue(1, where, isEarlier), nearestValue(2, where, isEarlier))
    if (velocity.isDefined) Some(velocity) else None
  }

  // ---- u, v and w on grids of their own ---------------------------------------------------

  // The nodes and levels of one variable's grid. In BRAN2020, u and v are at the corners of the
  // cells (xu_ocean, yu_ocean) and the centres of the layers (st_ocean), and w is at the centres
  // of the cells (xt_ocean, yt_ocean) and the bottoms of the layers (sw_ocean): half a cell, about
  // 5 km, and half a level from the others.
  private final class Axes(val longitudes: Array[Double], val latitudes: Array[Double], val depths: Array[Double])

  private lazy val axes: Array[Axes] = Array.tabulate(data.head.size) { c =>
    val (arrays, system) = data.head(c)
    new Axes(
      Array.tabulate(arrays(0)(0).length)(x => system.getLatLon(x, 0).getLongitude),
      Array.tabulate(arrays(0).length)(y => system.getLatLon(0, y).getLatitude),
      (if (componentDepths.size > c) componentDepths(c) else depths).toArray
    )
  }

  private def same(a: Array[Double], b: Array[Double]): Boolean =
    a.length == b.length && a.indices.forall(i => math.abs(a(i) - b(i)) < 1e-6)

  // the node of an axis, running either way, nearest a value; the end node for a value beyond it
  private def nearestIndex(nodes: Array[Double], value: Double): Int = {
    val ascending = nodes.last >= nodes.head
    var low = 0
    var high = nodes.length - 1
    while (high - low > 1) {
      val middle = (low + high) / 2
      if (if (ascending) nodes(middle) <= value else nodes(middle) >= value) low = middle else high = middle
    }
    if (math.abs(nodes(low) - value) <= math.abs(nodes(high) - value)) low else high
  }

  // Which earlier variable has the same nodes and levels, so that a position is only worked out once for both:
  // u and v share theirs in BRAN2020, and w has its own
  private lazy val sharesWith: Array[Int] = Array.tabulate(axes.length) { c =>
    (0 to c).find(earlier =>
      same(axes(earlier).longitudes, axes(c).longitudes) && same(axes(earlier).latitudes, axes(c).latitudes) &&
        same(axes(earlier).depths, axes(c).depths)
    ).get
  }

  /**
    * Where a position is among the nodes of each variable's grid, worked out once and used for the
    * earlier field and the later one: the nearest node and level, and, if asked for, the gap between
    * nodes it lies in with how far across it, for interpolating.
    */
  final class Location(count: Int) {
    val x = new Array[Int](count) // the nearest node's longitude and latitude index, and level
    val y = new Array[Int](count)
    val z = new Array[Int](count)
    val lonNode = new Array[Int](count) // the node before the position, or -1 if it is not between two
    val latNode = new Array[Int](count)
    val lonFraction = new Array[Double](count)
    val latFraction = new Array[Double](count)
  }

  def locate(coordinate: GeoCoordinate, withGeometry: Boolean): Location = {
    val where = new Location(axes.length)
    var c = 0
    while (c < axes.length) {
      val shared = sharesWith(c)
      if (shared < c) {
        where.x(c) = where.x(shared); where.y(c) = where.y(shared); where.z(c) = where.z(shared)
        where.lonNode(c) = where.lonNode(shared); where.latNode(c) = where.latNode(shared)
        where.lonFraction(c) = where.lonFraction(shared); where.latFraction(c) = where.latFraction(shared)
      } else {
        val a = axes(c)
        where.x(c) = nearestIndex(a.longitudes, coordinate.longitude)
        where.y(c) = nearestIndex(a.latitudes, coordinate.latitude)
        where.z(c) = closestIndex(a.depths, coordinate.depth)
        if (withGeometry) {
          where.lonNode(c) = gapStart(a.longitudes, where.x(c), coordinate.longitude)
          where.latNode(c) = gapStart(a.latitudes, where.y(c), coordinate.latitude)
          if (where.lonNode(c) >= 0 && where.latNode(c) >= 0) {
            where.lonFraction(c) = fractionAcross(a.longitudes, where.lonNode(c), coordinate.longitude)
            where.latFraction(c) = fractionAcross(a.latitudes, where.latNode(c), coordinate.latitude)
          }
        }
      }
      c += 1
    }
    where
  }

  private def field(c: Int, where: Location, isEarlier: Boolean): Array[Array[Float]] =
    (if (isEarlier) today else tomorrow)(c)(where.z(c))

  // the flow of one variable at the nearest node of its own grid; NaN on land
  private def nearestValue(c: Int, where: Location, isEarlier: Boolean): Double =
    field(c, where, isEarlier)(where.y(c))(where.x(c)).toDouble

  /**
    * The flow at a position from the nodes of each variable's own grid, blended in time as
    * `laterWeight` of the later field and the rest of the earlier (only the field that is needed
    * is read when that is 0 or 1). Each is interpolated bicubically where its 4 by 4 nodes all
    * have flow, else bilinearly among its 2 by 2, else from the nearest node; there is flow only
    * where all three have it.
    */
  def interpolatedVelocity(coordinate: GeoCoordinate, laterWeight: Double): Option[Velocity] = {
    val where = locate(coordinate, withGeometry = true)
    val components = new Array[Double](3) // u, v and w: the temperature, if there is one, is the fourth variable
    var c = 0
    while (c < 3) {
      components(c) = blended(c, where, laterWeight)
      if (components(c).isNaN) return None
      c += 1
    }
    Some(new Velocity(components(0), components(1), components(2)))
  }

  // One variable at a position, blended in time as `laterWeight` of the later field and the rest of the earlier
  private def blended(c: Int, where: Location, laterWeight: Double): Double =
    if (laterWeight <= 0.0) interpolatedValue(c, where, true)
    else if (laterWeight >= 1.0) interpolatedValue(c, where, false)
    else {
      val earlier = interpolatedValue(c, where, true)
      val later = interpolatedValue(c, where, false)
      // as Velocity does it: earlier * (1 - weight) + later * weight
      if (earlier.isNaN || later.isNaN) Double.NaN else earlier * (1.0 - laterWeight) + later * laterWeight
    }

  /** Whether the temperature was read as well as the flow. */
  def hasTemperature: Boolean = data.head.size > 3

  /**
    * The temperature, in degrees C, at a position, interpolated across the grid among the nodes of its own and
    * blended in time like the flow; None where there is none (land) or it was not read.
    */
  def temperature(coordinate: GeoCoordinate, laterWeight: Double): Option[Double] =
    if (!hasTemperature) None
    else {
      val value = blended(3, locate(coordinate, withGeometry = true), laterWeight)
      if (value.isNaN) None else Some(value)
    }

  // One variable at a position, in one field: NaN where it has no flow
  private def interpolatedValue(c: Int, where: Location, isEarlier: Boolean): Double = {
    val lonNode = where.lonNode(c)
    val latNode = where.latNode(c)
    if (lonNode >= 0 && latNode >= 0) {
      val a = axes(c)
      val values = field(c, where, isEarlier)
      val x = where.lonFraction(c)
      val y = where.latFraction(c)
      // bicubic, among the 4 by 4 nodes with the position in the gap between the middle ones
      if (lonNode >= 1 && latNode >= 1 && lonNode + 2 < a.longitudes.length && latNode + 2 < a.latitudes.length) {
        val x0 = lonNode - 1
        val y0 = latNode - 1
        val result = cubic(
          cubicRow(values, x0, y0, y), cubicRow(values, x0 + 1, y0, y), cubicRow(values, x0 + 2, y0, y), cubicRow(values, x0 + 3, y0, y), x)
        if (!result.isNaN) return result
      }
      // bilinear, among the 2 by 2 nodes
      val result = linear(linearRow(values, lonNode, latNode, y), linearRow(values, lonNode + 1, latNode, y), x)
      if (!result.isNaN) return result
    }
    nearestValue(c, where, isEarlier)
  }

  // the values of 4 nodes down a column of the grid, interpolated to a fraction of the way across the middle gap
  private def cubicRow(values: Array[Array[Float]], x: Int, y0: Int, fraction: Double): Double =
    cubic(values(y0)(x).toDouble, values(y0 + 1)(x).toDouble, values(y0 + 2)(x).toDouble, values(y0 + 3)(x).toDouble, fraction)

  private def linearRow(values: Array[Array[Float]], x: Int, y0: Int, fraction: Double): Double =
    linear(values(y0)(x).toDouble, values(y0 + 1)(x).toDouble, fraction)

  // the same arithmetic, in the same order, as CubicInterpolation and LinearInterpolation do on velocities
  private def cubic(v0: Double, v1: Double, v2: Double, v3: Double, x: Double): Double =
    v1 + (v2 - v0 + (v0 * 2.0 - v1 * 5.0 + v2 * 4.0 - v3 + ((v1 - v2) * 3.0 + v3 - v0) * x) * x) * x * 0.5

  private def linear(v0: Double, v1: Double, x: Double): Double = v0 + (v1 - v0) * x

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
  private def closestDepthIndex(depth: Double): Int = closestIndex(depthArray, depth)

  private def closestIndex(levels: Array[Double], depth: Double): Int = {
    var closest = 0
    var closestDistance = math.abs(levels(0) - depth)
    var i = 1
    while (i < levels.length) {
      val distance = math.abs(levels(i) - depth)
      if (distance < closestDistance) {
        closest = i
        closestDistance = distance
      }
      i += 1
    }
    closest
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

  /** Whether a position is on the grid, which getIndex fails for when it is not. */
  def covers(coordinate: GeoCoordinate): Boolean = {
    val index = data.head.head._2.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    isInsideGrid(index(0), index(1))
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
}

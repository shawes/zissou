package physical.flow

import exceptions.{CoordinateOutOfBoundsException, InterpolationNotImplementedException}
import grizzled.slf4j.Logging
import locals.Constants.NetcdfIndex
import locals._
import physical.adaptors.LatLonPointToGeoCoordinateAdaptor
import physical.{GeoCoordinate, Velocity}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid
import scala.collection.parallel.mutable._

class FlowGridWrapper(
    val depths: List[Double],
    val data: List[List[(Array[Array[Array[Float]]], GridCoordSystem)]]
) extends Logging {

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

  def getCentroid(index: (Int, Int, Int, Boolean)): GeoCoordinate = {
    LatLonPointToGeoCoordinateAdaptor.toGeoCoordinate(
      data.head.head._2.getLatLon(index._1, index._2)
    )
  }

  def getInterpolationValues(
      coordinate: GeoCoordinate,
      interpolation: InterpolationType
  ): Option[Array[Array[Velocity]]] =
    getInterpolationValues(coordinate, interpolation, getIndex(coordinate))

  /** As above, for a caller that has already looked up the coordinate's grid index. */
  def getInterpolationValues(
      coordinate: GeoCoordinate,
      interpolation: InterpolationType,
      index: (Int, Int, Int, Boolean)
  ): Option[Array[Array[Velocity]]] = {
    val quadrant = findQuadrantCoordinateIsIn(coordinate, index)
    val numberOfNeighbours = neighbourhoodSize(interpolation)
    val position = quadrantPosition(quadrant, numberOfNeighbours)
    neighbourhood(numberOfNeighbours, index, position._1, position._2)
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
    case _                   => throw new RuntimeException("Day not implemented")
  }

  private def neighbourhood(
      number: Int,
      gridIndex: (Int, Int, Int, Boolean),
      startIndexLat: Int,
      startIndexLon: Int
  ): Option[Array[Array[Velocity]]] = {
    val neighbourhood = new Array[Array[Velocity]](number)
    var row = 0
    while (row < number) {
      neighbourhood(row) = new Array[Velocity](number)
      row += 1
    }
    try {
      // One undefined (land) cell rules out the whole neighbourhood
      var undefined = false
      var j = startIndexLat
      while (!undefined && j < startIndexLat + number) {
        var i = startIndexLon
        while (!undefined && i < startIndexLon + number) {
          velocityAt(gridIndex._1 + i, gridIndex._2 + j, gridIndex._3, true) match {
            case Some(velocity) =>
              neighbourhood(i - startIndexLon)(j - startIndexLat) = velocity
            case None => undefined = true
          }
          i += 1
        }
        j += 1
      }
      if (undefined) None else Some(neighbourhood)
    } catch {
      case ex: IndexOutOfBoundsException => None
    }
  }

  private def findQuadrantCoordinateIsIn(
      coordinate: GeoCoordinate,
      index: (Int, Int, Int, Boolean)
  ): QuadrantLocation = {
    val centroid = data.head.head._2.getLatLon(index._1, index._2)

    if (coordinate.latitude > centroid.getLatitude) {
      if (coordinate.longitude < centroid.getLongitude) {
        TopLeft
      } else {
        TopRight
      }
    } else {
      if (coordinate.longitude < centroid.getLongitude) {
        BottomLeft
      } else {
        BottomRight
      }
    }
    //}
  }

  private def quadrantPosition(
      quadrant: QuadrantLocation,
      size: Int
  ): (Int, Int) =
    quadrant match {
      case TopLeft    => (-1 * size / 2, -1 * size / 2)
      case TopRight   => ((-1 * size / 2) + 1, -1 * size / 2)
      case BottomLeft => (-1 * size / 2, (-1 * size / 2) + 1)
      case BottomRight =>
        ((-1 * size / 2) + 1, (-1 * size / 2) + 1)
      case _ => throw new RuntimeException("Wrong quadrant")
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

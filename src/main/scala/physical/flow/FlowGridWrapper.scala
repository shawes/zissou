package physical.flow

import exceptions.{InterpolationNotImplementedException, NotEnoughNeighbouringCellsException, UndefinedVelocityException}
import grizzled.slf4j.Logging
import locals.Interpolation.Interpolation
import locals.Quadrant.Quadrant
import locals.{Constants, Interpolation, Quadrant}
import physical.adaptors.LatLonPointToGeoCoordinateAdaptor
import physical.{GeoCoordinate, Velocity}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid

class FlowGridWrapper(val gcs: GridCoordSystem, val depths: List[Double], val datasets: List[GeoGrid]) extends Logging {

  val X = 0
  val Y = 1
  val Z = 2
  val T = 3

  def getVelocity(coordinate: GeoCoordinate): Velocity = {
    val gridIndex = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val depthIndex = closestDepthIndex(coordinate.depth)
    debug("Reading depth" + depthIndex + " gridY " + gridIndex(Y) + "gridx" + gridIndex(X))
    val data = datasets.map(dataset => dataset.readDataSlice(0, depthIndex, gridIndex(Y), gridIndex(X)).getDouble(0))
    debug(data.foreach(x => x.toString))
    new Velocity(data.head, data(1), data(2))
  }

  private def closestDepthIndex(depth: Double): Int = depths match {
    case Nil => Int.MaxValue
    case list => list.indexOf(list.minBy(v => math.abs(v - depth)))
  }

  def getCentroid(index: Array[Int]): GeoCoordinate = {
    LatLonPointToGeoCoordinateAdaptor.toGeoCoordinate(gcs.getLatLon(index(X), index(Y)))
  }

  def getInterpolationValues(coordinate: GeoCoordinate, interpolation: Interpolation): Array[Array[Velocity]] = {
    val quadrat = findQuadratCoordinateIsIn(coordinate)
    val numberOfNeighbours = neighbourhoodSize(interpolation)
    val position = quadrantPosition(quadrat, numberOfNeighbours)

    try {
      neighbourhood(numberOfNeighbours, getIndex(coordinate), position._1, position._2)
    } catch {
      case ex: NotEnoughNeighbouringCellsException => getInterpolationValues(coordinate, nextInterpolationStep(interpolation))
    }
  }

  def getIndex(coordinate: GeoCoordinate): Array[Int] = {
    val indexXY = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val indexZ = closestDepthIndex(coordinate.depth)
    Array(indexXY(0), indexXY(1), indexZ, 0)
  }

  def getVelocity(index: Array[Int]): Velocity = {
    debug("Length is " + index.length)
    val data = datasets.map(dataset => dataset.readDataSlice(0, index(Z), index(Y), index(X)).getFloat(0))
    debug(data.foreach(x => x.toString))
    new Velocity(data.head, data(1), data(2))
  }

  private def neighbourhood(number: Int, gridIndex: Array[Int], startIndexLat: Int, startIndexLon: Int): Array[Array[Velocity]] = {
    val neighbourhood = Array.ofDim[Velocity](number, number)
    for (j <- startIndexLat to (startIndexLat + number)) {
      for (i <- startIndexLon to startIndexLon + number) {
        val velocity = getVelocity(Array(gridIndex(X) + i, gridIndex(Y) + j, gridIndex(Z), 0))
        if (velocity.isDefined) {
          neighbourhood(i - startIndexLon)(j - startIndexLat) = velocity
        } else {
          throw new NotEnoughNeighbouringCellsException()
        }
      }
    }
    neighbourhood
  }

  private def findQuadratCoordinateIsIn(coordinate: GeoCoordinate): Quadrant = {
    val index = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val centroid = gcs.getLatLon(index(X), index(Y))

    if (coordinate.latitude > centroid.getLatitude) {
      if (coordinate.longitude < centroid.getLongitude) {
        Quadrant.TopLeft
      } else {
        Quadrant.TopRight
      }
    } else {
      if (coordinate.longitude < centroid.getLongitude) {
        Quadrant.BottomLeft
      } else {
        Quadrant.BottomRight
      }
    }
  }

  private def quadrantPosition(quadrant: Quadrant, size: Int): (Int, Int) = quadrant match {
    case Quadrant.TopLeft => (-1 * size / 2, -1 * size / 2)
    case Quadrant.TopRight => ((-1 * size / 2) + 1, -1 * size / 2)
    case Quadrant.BottomLeft => (-1 * size / 2, (-1 * size / 2) + 1)
    case Quadrant.BottomRight => ((-1 * size / 2) + 1, (-1 * size / 2) + 1)
  }

  private def neighbourhoodSize(interpolation: Interpolation): Int = interpolation match {
    case Interpolation.Bilinear => math.sqrt(Constants.Interpolation.CubicPoints).toInt
    case Interpolation.Bicubic => math.sqrt(Constants.Interpolation.BicubicPoints).toInt
    case Interpolation.Tricubic => math.sqrt(Constants.Interpolation.TricubicPoints).toInt
    case _ => throw new InterpolationNotImplementedException()
  }

  private def nextInterpolationStep(interpolation: Interpolation): Interpolation = interpolation match {
    case Interpolation.Bicubic => Interpolation.Bilinear
    case Interpolation.Tricubic => Interpolation.TriLinear
    case _ => throw new UndefinedVelocityException()
  }






}

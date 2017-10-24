package physical.flow

import exceptions.InterpolationNotImplementedException
import grizzled.slf4j.Logging
import locals.Constants.NetcdfIndex
import locals.Day._
import locals.InterpolationType.InterpolationType
import locals.QuadrantType.QuadrantType
import locals.{Constants, InterpolationType, QuadrantType}
import physical.adaptors.LatLonPointToGeoCoordinateAdaptor
import physical.{GeoCoordinate, Velocity}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid
import scala.collection.parallel.mutable._

class FlowGridWrapper(val depths: List[Double], val data: List[List[(Array[Array[Array[Float]]],GridCoordSystem)]]) extends Logging {


  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    val gridIndex = getIndex(coordinate)
    debug("getVelocity: Index is "+ gridIndex)
    getVelocity(gridIndex)
  }

  def getVelocity(index: (Int,Int,Int,Boolean)): Option[Velocity] = {
    val buildVelocity = index._4 match {
      case true => data.head.map(datasets => readData(datasets._1,index))
      case false => data.last.map(datasets => readData(datasets._1,index))
    }
    val velocity = new Velocity(buildVelocity.head.toDouble, buildVelocity(1).toDouble, buildVelocity(2).toDouble)
    if (velocity.isDefined) Some(velocity) else None
  }

  private def readData(dataset : Array[Array[Array[Float]]], index: (Int,Int,Int,Boolean)): Float = {
    dataset(index._3)(index._2)(index._1)
  }



  private def closestDepthIndex(depth: Double): Int = {
    depths.map(v => math.abs(v - depth)).zipWithIndex.min._2
  }

  def getCentroid(index: (Int,Int,Int,Boolean)): GeoCoordinate = {
    LatLonPointToGeoCoordinateAdaptor.toGeoCoordinate(data.head.head._2.getLatLon(index._1, index._2))
  }

  def getInterpolationValues(coordinate: GeoCoordinate, interpolation: InterpolationType): Option[Array[Array[Velocity]]] = {
    val quadrat = findQuadratCoordinateIsIn(coordinate)
    val numberOfNeighbours = neighbourhoodSize(interpolation)
    val position = quadrantPosition(quadrat, numberOfNeighbours)
    neighbourhood(numberOfNeighbours, getIndex(coordinate), position._1, position._2)
  }

  def getIndex(coordinate: GeoCoordinate): (Int,Int,Int,Boolean) = {
    getIndex(coordinate, Today)
  }

  def getIndex(coordinate: GeoCoordinate, day: Day): (Int,Int,Int,Boolean) = {
    //this.synchronized {
    val indexXY = data.head.head._2.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val indexZ = closestDepthIndex(coordinate.depth)
    val indexT = timeIndex(day)
    debug("Coord is :" + coordinate)
    debug("x="+indexXY(0)+",y="+indexXY(1) +",z="+indexZ+",t="+indexT)
    // (x, y, z ,t)
    (indexXY(0), indexXY(1), indexZ, indexT)
  //}
  }

  private def timeIndex(day: Day): Boolean = day match {
    case Today => true
    case Tomorrow => false
    case _ => throw new RuntimeException("Day not implemented")
  }

  private def neighbourhood(number: Int, gridIndex: (Int,Int,Int,Boolean), startIndexLat: Int, startIndexLon: Int): Option[Array[Array[Velocity]]] = {
    val neighbourhood = Array.ofDim[Velocity](number, number)
    try {
      for (j <- startIndexLat until (startIndexLat + number)) {
        for (i <- startIndexLon until (startIndexLon + number)) {
          neighbourhood(i - startIndexLon)(j - startIndexLat) =
            getVelocity((gridIndex._1 + i, gridIndex._2 + j, gridIndex._3, true)).getOrElse(new Velocity(Double.NaN, Double.NaN))
        }
      }
      if (neighbourhood.flatten.exists(v => v.isUndefined)) {
        None
      } else {
        Some(neighbourhood)
      }
    } catch {
      case ex: IndexOutOfBoundsException => None
    }
  }



  private def findQuadratCoordinateIsIn(coordinate: GeoCoordinate): QuadrantType = {
    //this.synchronized {
    val index = getIndex(coordinate)
    val centroid = data.head.head._2.getLatLon(index._1, index._2)

    if (coordinate.latitude > centroid.getLatitude) {
      if (coordinate.longitude < centroid.getLongitude) {
        debug("TL")
        QuadrantType.TopLeft
      } else {
        debug("TR")
        QuadrantType.TopRight
      }
    } else {
      if (coordinate.longitude < centroid.getLongitude) {
        debug("BL")
        QuadrantType.BottomLeft
      } else {
        debug("BR")
        QuadrantType.BottomRight
      }
    }
  //}
  }

  private def quadrantPosition(quadrant: QuadrantType, size: Int): (Int, Int) = quadrant match {
    case QuadrantType.TopLeft => (-1 * size / 2, -1 * size / 2)
    case QuadrantType.TopRight => ((-1 * size / 2) + 1, -1 * size / 2)
    case QuadrantType.BottomLeft => (-1 * size / 2, (-1 * size / 2) + 1)
    case QuadrantType.BottomRight => ((-1 * size / 2) + 1, (-1 * size / 2) + 1)
    case _ => throw new RuntimeException("Wrong quadrant")
  }

  private def neighbourhoodSize(interpolation: InterpolationType): Int = interpolation match {
    case InterpolationType.Bilinear => math.sqrt(Constants.Interpolation.CubicPoints).toInt
    case InterpolationType.Bicubic => math.sqrt(Constants.Interpolation.BicubicPoints).toInt
    case InterpolationType.Tricubic => math.sqrt(Constants.Interpolation.TricubicPoints).toInt
    case _ => throw new InterpolationNotImplementedException()
  }







}

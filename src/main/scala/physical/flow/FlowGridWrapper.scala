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

class FlowGridWrapper(val gcs: GridCoordSystem, val depths: List[Double], val datasets: List[GeoGrid]) extends Logging {

  val data : List[(Array[Array[Array[Float]]],GridCoordSystem)] = datasets.map(dataset => (dataset.readDataSlice(0,-1,-1,-1).copyToNDJavaArray().asInstanceOf[Array[Array[Array[Float]]]], dataset.getCoordinateSystem))

  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
    val gridIndex = data.head._2.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    if(gridIndex(0) != -1 && gridIndex(1) != -1) {
      val depthIndex = closestDepthIndex(coordinate.depth)
      val velocityData = data.map(array => array._1(depthIndex)(gridIndex(NetcdfIndex.Y))(gridIndex(NetcdfIndex.X)))
      val velocity = new Velocity(velocityData.head, velocityData(1), velocityData(2))
      if (velocity.isDefined) Some(velocity) else None
    } else {
      None
    }
  }

  private def closestDepthIndex(depth: Double): Int = depths match {
    case Nil => Int.MaxValue
    case list => list.indexOf(list.minBy(v => math.abs(v - depth)))
  }

  def getCentroid(index: Array[Int]): GeoCoordinate = {
    LatLonPointToGeoCoordinateAdaptor.toGeoCoordinate(gcs.getLatLon(index(NetcdfIndex.X), index(NetcdfIndex.Y)))
  }

  def getInterpolationValues(coordinate: GeoCoordinate, interpolation: InterpolationType): Option[Array[Array[Velocity]]] = {
    val quadrat = findQuadratCoordinateIsIn(coordinate)
    val numberOfNeighbours = neighbourhoodSize(interpolation)
    val position = quadrantPosition(quadrat, numberOfNeighbours)
    neighbourhood(numberOfNeighbours, getIndex(coordinate), position._1, position._2)
  }

  def getIndex(coordinate: GeoCoordinate): Array[Int] = {
    getIndex(coordinate, Today)
  }

  def getIndex(coordinate: GeoCoordinate, day: Day): Array[Int] = {
    //this.synchronized {
    val indexXY = data.head._2.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val indexZ = closestDepthIndex(coordinate.depth)
    val indexT = timeIndex(day)
    //debug("x="+indexXY(0)+",y="+indexXY(1) +",z="+indexZ+",t="+indexT)
    Array(indexXY(0), indexXY(1), indexZ, indexT)
  //}
  }

  private def timeIndex(day: Day): Int = day match {
    case Today => 0
    case Tomorrow => 1
    case _ => throw new RuntimeException("Day not implemented")
  }

  private def neighbourhood(number: Int, gridIndex: Array[Int], startIndexLat: Int, startIndexLon: Int): Option[Array[Array[Velocity]]] = {
    val neighbourhood = Array.ofDim[Velocity](number, number)
    try {
      for (j <- startIndexLat until (startIndexLat + number)) {
        for (i <- startIndexLon until (startIndexLon + number)) {
          neighbourhood(i - startIndexLon)(j - startIndexLat) =
            getVelocity(Array(gridIndex(NetcdfIndex.X) + i, gridIndex(NetcdfIndex.Y) + j, gridIndex(NetcdfIndex.Z), 0)).getOrElse(new Velocity(Double.NaN, Double.NaN))
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

  def getVelocity(index: Array[Int]): Option[Velocity] = {
    val velocityData = data.map(array => array._1(NetcdfIndex.Z)(index(NetcdfIndex.Y))(index(NetcdfIndex.X)))
    val velocity = new Velocity(velocityData.head, velocityData(1), velocityData(2))
    if (velocity.isDefined) Some(velocity) else None
  }

  private def findQuadratCoordinateIsIn(coordinate: GeoCoordinate): QuadrantType = {
    //this.synchronized {
    val index = data.head._2.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val centroid = data.head._2.getLatLon(index(NetcdfIndex.X), index(NetcdfIndex.Y))

    if (coordinate.latitude > centroid.getLatitude) {
      if (coordinate.longitude < centroid.getLongitude) {
        QuadrantType.TopLeft
      } else {
        QuadrantType.TopRight
      }
    } else {
      if (coordinate.longitude < centroid.getLongitude) {
        QuadrantType.BottomLeft
      } else {
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

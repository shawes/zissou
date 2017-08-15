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

  def getVelocity(coordinate: GeoCoordinate): Option[Velocity] = {
  //  this.synchronized {
    val gridIndex = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    if(gridIndex(0) != -1 && gridIndex(1) != -1) {
    val depthIndex = closestDepthIndex(coordinate.depth)
    //debug("Reading depth" + depthIndex + " gridY " + gridIndex(NetcdfIndex.Y) + "gridx" + gridIndex(NetcdfIndex.X))
    val data = datasets.map(dataset => dataset.readDataSlice(0, depthIndex,
                gridIndex(NetcdfIndex.Y), gridIndex(NetcdfIndex.X)).getDouble(0))


    val velocity = new Velocity(data.head, data(1), data(2))
    if (velocity.isDefined) Some(velocity) else None
  } else {
    None
  }
//}
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
    val indexXY = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
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
    //debug("Grid Index is x="+gridIndex(NetcdfIndex.X)+",y="+gridIndex(NetcdfIndex.Y) +",z="+NetcdfIndex.Z)
    //this.synchronized {
    val neighbourhood = Array.ofDim[Velocity](number, number)
    try {
      for (j <- startIndexLat until (startIndexLat + number)) {
        for (i <- startIndexLon until (startIndexLon + number)) {
          //debug("i,j: " + i + "," + j)
          //debug("lon,lat: " + startIndexLon + "," + startIndexLat)
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
  //}


  }

  def getVelocity(index: Array[Int]): Option[Velocity] = {
//this.synchronized {
      //debug("Grid Index is x="+index(NetcdfIndex.X)+",y="+index(NetcdfIndex.Y) +",z="+index(NetcdfIndex.Z))
//    val data = datasets.map(dataset => dataset.readDataSlice(0, index(NetcdfIndex.Z),      index(NetcdfIndex.Y), index(NetcdfIndex.X)).getFloat(0))

    val u = datasets(0).readDataSlice(0, index(NetcdfIndex.Z), index(NetcdfIndex.Y),     index(NetcdfIndex.X)).getDouble(0)

    val v = datasets(1).readDataSlice(0, index(NetcdfIndex.Z), index(NetcdfIndex.Y),     index(NetcdfIndex.X)).getFloat(0)

    val w = datasets(2).readDataSlice(0, index(NetcdfIndex.Z), index(NetcdfIndex.Y),     index(NetcdfIndex.X)).getFloat(0)
    //debug("Index is: z="+ index(NetcdfIndex.Z).toString +",y="+index(NetcdfIndex.Y).toString +",z="+ index(NetcdfIndex.X).toString)
    //debug("Data is: "+ data.head.toString +","+data(1).toString +","+ data(2).toString )
    //val velocity = new Velocity(data.head.toDouble, data(1).toDouble, data(2).toDouble)
    val velocity = new Velocity(u,v,w)
    if (velocity.isDefined) Some(velocity) else None
  //}
  }

  private def findQuadratCoordinateIsIn(coordinate: GeoCoordinate): QuadrantType = {
    //this.synchronized {
    val index = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val centroid = gcs.getLatLon(index(NetcdfIndex.X), index(NetcdfIndex.Y))

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

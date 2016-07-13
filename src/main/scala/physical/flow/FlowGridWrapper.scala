package physical.flow

import physical.adaptors.LatLonPointToGeoCoordinateAdaptor
import physical.{GeoCoordinate, Velocity}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid

class FlowGridWrapper(val gcs: GridCoordSystem, val depths: List[Double], val datasets: List[GeoGrid]) {

  val X = 0
  val Y = 1
  val Z = 2

  def getVelocity(coordinate: GeoCoordinate): Velocity = {
    val gridIndex = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val depthIndex = getClosestDepthIndex(coordinate.depth)
    val data = datasets.map(dataset => dataset.readDataSlice(0, depthIndex, gridIndex(Y), gridIndex(X)).getFloat(0))
    new Velocity(data.head, data(1), data(2))
  }

  private def getClosestDepthIndex(depth: Double): Int = depths match {
    case Nil => Int.MaxValue
    case list => list.indexOf(list.minBy(v => math.abs(v - depth)))
  }

  def getVelocity(index: Array[Int]): Velocity = {
    val data = datasets.map(dataset => dataset.readDataSlice(0, index(Z), index(Y), index(X)).getFloat(0))
    new Velocity(data.head, data(1), data(2))
  }

  def getIndex(coordinate: GeoCoordinate): Array[Int] = {
    val indexXY = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val indexZ = getClosestDepthIndex(coordinate.depth)
    indexXY :+ indexZ
  }

  def getCentroid(index: Array[Int]): GeoCoordinate = {
    LatLonPointToGeoCoordinateAdaptor.toGeoCoordinate(gcs.getLatLon(index(X), index(Y)))
  }

}

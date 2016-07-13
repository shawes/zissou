package physical.flow

import grizzled.slf4j.Logging
import physical.adaptors.LatLonPointToGeoCoordinateAdaptor
import physical.{GeoCoordinate, Velocity}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid

class FlowGridWrapper(val gcs: GridCoordSystem, val depths: List[Double], val uDataset: GeoGrid, val vDataset: GeoGrid, val wDataset: GeoGrid) extends Logging {

  val X = 0
  val Y = 1
  val Z = 2

  def getVelocity(coordinate: GeoCoordinate): Velocity = {
    val gridIndex = gcs.findXYindexFromLatLon(coordinate.latitude, coordinate.longitude, null)
    val depthIndex = getClosestDepthIndex(coordinate.depth)
    debug("depthIndex = " + depthIndex + " and gridIndex_Y = " + gridIndex(Y) + " gridIdex_X =" + gridIndex(X))
    val u = uDataset.readDataSlice(0, 0, gridIndex(Y), gridIndex(X)).getFloat(0)
    val v = vDataset.readDataSlice(0, 0, gridIndex(Y), gridIndex(X)).getFloat(0)
    val w = wDataset.readDataSlice(0, 0, gridIndex(Y), gridIndex(X)).getFloat(0)
    new Velocity(u, v, w)
  }

  private def getClosestDepthIndex(depth: Double): Int = depths match {
    case Nil => Int.MaxValue
    case list => list.indexOf(list.minBy(v => math.abs(v - depth)))
  }

  def getVelocity(index: Array[Int]): Velocity = {
    val u = uDataset.readDataSlice(0, 0, index(Y), index(X)).getFloat(0)
    val v = vDataset.readDataSlice(0, 0, index(Y), index(X)).getFloat(0)
    val w = wDataset.readDataSlice(0, 0, index(Y), index(X)).getFloat(0)
    new Velocity(u, v, w)
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

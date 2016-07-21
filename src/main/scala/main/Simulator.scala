package main

import java.io.File

import grizzled.slf4j.Logging
import io.NetcdfFileHandler
import io.config.{Configuration, ConfigurationFile}
import ucar.ma2.Range
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}


object Simulator extends App with Logging {

  //val model = new CoupledBiophysicalModel(readConfigurationFile)
  //model.run()

  val netcdfReader = new NetcdfFileHandler()
      val datasetU = netcdfReader.openLocalFile("test-data/netcdf/u/ocean_u_2011_12.nc").findGridByName("u")
      val datasetV = netcdfReader.openLocalFile("test-data/netcdf/v/ocean_v_2011_12.nc").findGridByName("v")
    val datasets = List(datasetU,datasetV)

    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))

    //val newBounds: LatLonRect = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))
    val timeRange = new Range(0, 1)
  val depthRange = new Range(0, 15)
    //val day1 = grid.subset(timeRange, depthRange, latlonBounds, 0, 0, 0)
    val gcs = datasetU.getCoordinateSystem
    //GridDatatype grid = gds.findGridDatatype( args[1]);
    //GridCoordSystem gcs = grid.getCoordinateSystem();

    val xAxis = gcs.getXHorizAxis
    val yAxis = gcs.getYHorizAxis
    val zAxis = gcs.getVerticalAxis// may be null

    if (gcs.hasTimeAxis1D) {
      val tAxis1D = gcs.getTimeAxis1D
      val dates = tAxis1D.getCalendarDates
    } else if (gcs.hasTimeAxis) {
      val tAxis = gcs.getTimeAxis
    }

    val subsets = datasets.map(d => d.subset(timeRange,depthRange,latlonBounds,0,0,0))

    //val gcs = datasetU.getCoordinateSystem
    val toowoomba = gcs.findXYindexFromLatLon(-24.55, 153.101,null)
    println("Toowoomba is the XY coordinates " + toowoomba(0) + "," + toowoomba(1))
    val toowoombaVelocitys = datasets.map(data => data.readDataSlice(0, 0, toowoomba(1), toowoomba(0)))
    println("The current in towoomba is u: " + toowoombaVelocitys.head.getDouble(0) + " v:" + toowoombaVelocitys(1).getDouble(0))

    val gcs_subset = subsets.head.getCoordinateSystem
    val toowoomba_subset = gcs_subset.findXYindexFromLatLon(-24.55, 153.101,null)
    println("Toowoomba is the XY coordinates " + toowoomba_subset(0) + "," + toowoomba_subset(1))
  val toowoomba_subset_below = gcs_subset.findXYindexFromLatLon(-24.55, 153.2, null)
  println("Toowoomba below is the XY coordinates " + toowoomba_subset_below(0) + "," + toowoomba_subset_below(1))
  val toowoomba_subset_left = gcs_subset.findXYindexFromLatLon(-24.65, 153.101, null)
  println("Toowoomba left is the XY coordinates " + toowoomba_subset_left(0) + "," + toowoomba_subset_left(1))
    val toowoombaVelocitys_subset = subsets.map(data => data.readDataSlice(1, 3, toowoomba_subset(1), toowoomba_subset(0)))
    println("The current in towoomba is u: " + toowoombaVelocitys_subset.head.getDouble(0) + " v:" + toowoombaVelocitys_subset(1).getDouble(0))

    val u = datasetU.readDataSlice(0, 0, toowoomba(1), toowoomba(0))
    println("u is"+ u)

    //val data = datasetU.readDataSlice(0, 0, 10, 20)
    //println("data is"+ data)

    // find the x,y index for a specific lat/lon position
    val xy : Array[Int] = gcs.findXYindexFromLatLon(-24.55,153.119, null) // xy[0] = x, xy[1] = y

    // read the data at that lat, lon and the first time and z level (if any)
    val data  = datasetU.readDataSlice(0, 0, xy(1), xy(0)) // note order is t, z, y, x
    val result = data.getDouble(0) // we know its a scalar
  println("data is " + data)

  for (j <- -2 to 1) {
    for (i <- -2 to 1) {
      print("i=" + i + ",j=" + j + " ")
      val data = datasetU.readDataSlice(0, 0, xy(1) + j, xy(0) + i)
      print("data is " + data)
    }
    println()
  }



  private def readConfigurationFile: Configuration = {
    val configurationFile = new ConfigurationFile()
    val testConfigPathDesktop = args(0)
    configurationFile.read(new File(testConfigPathDesktop))
  }
}

package io

import java.io.IOException

import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import ucar.ma2.Range
import ucar.nc2.dt.grid.{GeoGrid, GridDataset}
import ucar.unidata.geoloc.{LatLonPointImpl, LatLonRect}

//import org.apache.commons.httpclient.{UsernamePasswordCredentials, Credentials}
//import org.apache.commons.httpclient.auth.{AuthScheme, CredentialsNotAvailableException, CredentialsProvider}
//import org.apache.http.auth.UsernamePasswordCredentials
import ucar.httpservices.HTTPSession
import ucar.nc2.NetcdfFile

//import ucar.nc2.util.net.{HTTPAuthScheme, HTTPSession, HttpClientManager}


class NetcdfFileHandler {


  //setCredentials()
  //readLocalFile()
  //readUrlFile()
  //readOpendapFile()
  //readThreddsFile()


  def setCredentials(): Unit = {
    //val session = new HTTPSession()
    //val provider: CredentialsProvider = new ZissouCredentialsProvider()
    // HttpClientManager.init(provider, null)
    //HTTPSession.setGlobalCredentialsProvider(HTTPAuthScheme.BASIC, provider)
    //HTTPSession.setGlobalCredentials(HTTPAuthScheme.BASIC, )
    //HTTPSession.setGlobalCredentials(HTTPAuthScheme.BASIC, user)

    HTTPSession.setGlobalCredentials(new AuthScope("https://www.cmar.csiro.au/", 443), new UsernamePasswordCredentials("noncom29", "JwkPF0"))
    HTTPSession.setGlobalUserAgent("netcdf/java")
    // HTTPSession.setMaxConnections(4)
    //HTTPSession.setGlobalAuthenticationPreemptive(true)
  }

  def openLocalFile(file: String): GeoGrid = {
    var grid: GeoGrid = null
    try {
      val gridFile = GridDataset.open(file)
      grid = gridFile.getGrids.get(0).asInstanceOf[GeoGrid]
      gridFile.close()
    } catch {
      case ioe: IOException => println("Error opening the " + file + ", getting exception: " + ioe.getMessage)
    }
    grid
  }

  def readLocalFile(): Unit = {
    val filename = "test-data/netcdf/ocean_u_2011_12.nc"
    try {
      val gridFile = GridDataset.open(filename)
      val grid: GeoGrid = gridFile.getGrids.get(0).asInstanceOf[GeoGrid]
      process(grid)
      println("Read the local file as a grid")
      gridFile.close()
    } catch {
      case ioe: IOException => println("Trying to open " + filename + ", but " + ioe.getMessage)
    }
  }

  def process(grid: GeoGrid): Unit = {
    val varName = "u"
    //val v = grid.findVariable(varName)
    //val origin = Array(0,0,0,0)
    println("Starting read")
    val shape = grid.getShape
    val dimensions = grid.getDimensions
    //val range = grid.getRanges
    val latlonBounds = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))

    //val newBounds: LatLonRect = new LatLonRect(new LatLonPointImpl(-40.0, 142.0), new LatLonPointImpl(-10.0, 162.0))
    val timeRange: Range = new Range(0, 1)
    val depthRange: Range = new Range(0, 15)
    val day1 = grid.subset(timeRange, depthRange, latlonBounds, 0, 0, 0)
    println("read in Day 1")
    //val day2 = grid.subset(new Range(1,2), depthRange, latlonBounds, 0, 0, 0)
    //println("read in Day 2")
    val data = day1.readDataSlice(0, 0, 10, 20)
    println("read in data of size " + data.getSize)
    println("the value is " + data.getFloat(0))



    val gcs = day1.getCoordinateSystem
    val result = gcs.findXYindexFromLatLon(-32, 151.0, null)
    println("The result is the XY coordinates " + result(0) + "," + result(1))

    val result2 = gcs.findXYindexFromLatLon(-32.1, 151.0, null)
    println("The result2 is the XY coordinates " + result2(0) + "," + result2(1))
    val result3 = gcs.findXYindexFromLatLon(-32.1, 151.1, null)
    println("The result3 is the XY coordinates " + result3(0) + "," + result3(1))

    val toowoomba = gcs.findXYindexFromLatLon(-27.34, 151.57, null)
    println("Toowoomba is the XY coordinates " + toowoomba(0) + "," + toowoomba(1))
    val toowoombaVelocity = day1.readDataSlice(0, 0, toowoomba(1), toowoomba(0))
    println("The current in towoomba is: " + toowoombaVelocity)
    //day1.readDataSlice(0,0,resu)


    //v.read

  }

  def readUrlFile(): Unit = {
    val url = "http://www.cmar.csiro.au/thredds/fileServer/BRAN3p5/u/ocean_u_2011_12.nc"
    // Set some parameters at the global level

    try {
      val ncfile = NetcdfFile.open(url)
      //process(ncfile)
      println("Read the online file")
      ncfile.close()
    } catch {
      case ioe: IOException => println("Trying to open " + url + ", but " + ioe.getMessage)
      //case ice: org.apache.http.auth.InvalidCredentialsException => println(ice.getMessage)
    }
  }

  def readOpendapFile(): Unit = {
    val url = "dods://www.cmar.csiro.au/thredds/dodsC/BRAN3p5/u/ocean_u_2011_12.nc#bran3p5_u/ocean_u_2011_12.nc"
    // NetcdfDataset ncd = null;
    try {
      val gcd = GridDataset.open(url)

      //process(ncd)
      gcd.close()
      println("Read the opendap file")
    } catch {
      case ioe: IOException => println("Trying to open " + url + ", but " + ioe.getMessage)
    }
  }

  def readThreddsFile(): Unit = {
    val url = "thredds:http://www.cmar.csiro.au/thredds/catalog/BRAN3p5/u/catalog.xml#bran3p5_u/ocean_u_2011_12.nc"
    //val url = "thredds://www.cmar.csiro.au/thredds/catalog.xml"
    //val url = "http://ogc-hba.vm.csiro.au/thredds/catalog/BRAN3p5/u/latest.xml#bran3p5_u/ocean_u_2011_12.nc"
    //val url = "thredds:https://www.cmar.csiro.au/thredds/catalog/BRAN3p5/u/latest.xml#bran3p5_u/ocean_u_2012_07.nc"
    // NetcdfDataset ncd = null;
    try {
      //val ncd = NetcdfDataset.openDataset(url)
      val gcd = GridDataset.open(url)

      //process(ncd)
      gcd.close()
      println("Read the thredds file")
    } catch {
      case ioe: IOException => println("Trying to open " + url + ", but " + ioe.getMessage)
    }
  }
}

/*class ZissouCredentialsProvider extends CredentialsProvider     {
  @throws(classOf[CredentialsNotAvailableException])
  override def getCredentials(scheme: AuthScheme, host: String, port: Int, proxy: Boolean): Credentials = {
      System.out.println("Called Credentials Provider!")
      throw new CredentialsNotAvailableException()
  }
}*/

//class ZissouCredentials extends Credentials {

//}

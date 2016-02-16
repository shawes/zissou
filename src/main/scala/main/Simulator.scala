package main

import java.io.File

import grizzled.slf4j.Logging
import io.ConfigurationFileReader

object Simulator extends App with Logging {

  try {

    //val sydney = new GeoCoordinate(-33.865143,151.209900)
    //val orange = new GeoCoordinate(-33.283577,149.101273)

    // val known_dist = 69.0

    //val calc_dist = GeometryToGeoCoordinateAdaptor.toPoint(sydney).distance(GeometryToGeoCoordinateAdaptor.toPoint(orange))

    //info("The calculate distance is: "+ calc_dist)
    //val calc_dist2 =  calc_dist * (Math.PI / 180) * 6378137
    //info("Converted to radians is: " + calc_dist2 )
    //System.exit(0)
    //info("Using conversion = " + )

    //    var chooser = new FileChooser(new File("/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion"))
    //    chooser.multiSelectionEnabled_=(b = false)
    //    chooser.showOpenDialog(null)
    val configFileReader = new ConfigurationFileReader()
    //    val config = configFileWriter.read(chooser.selectedFile)

    //val testConfigPathLaptop = "/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion/test_config.xml"
    val testConfigPathDesktop = args(0)

    val config = configFileReader.read(new File(testConfigPathDesktop))
    info("Configuration loaded")
    val model = new CoupledBiophysicalModel(config)





    /*    val filename = "http://noncom29:JwkPF0@www.cmar.csiro.au/thredds/dodsC/BRAN2.1/u/ocean_u_2002_10_14.nc"


        val session = new HTTPSession(filename)
        val method: HTTPMethod = HTTPMethod.Get(session)
        val status : Int = method.execute()
        System.out.printf("Execute: status code = %d\n", status)
        method.close()
        session.close()

        var ncfile : NetcdfFile = null
        try {
          ncfile = NetcdfDataset.openFile(filename, null)
          ///process( ncfile);
        } catch {
          case ioe: IOException =>
            error("trying to open " + filename, ioe);
        } finally {
          if (null != ncfile) try {
            ncfile.close()
          } catch {
            case ioe: IOException =>
            error("trying to close " + filename, ioe);
          }
        }*/

    model.run()


  } catch {
    case ex: IllegalArgumentException => new Error("Bugger")
    //case ex: Exception => System.exit(0)
  }


}

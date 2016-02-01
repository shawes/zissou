package main

import java.io.File

import grizzled.slf4j.Logging
import io.ConfigurationFileReader

object Simulator extends App with Logging {

  try {



    //    var chooser = new FileChooser(new File("/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion"))
    //    chooser.multiSelectionEnabled_=(b = false)
    //    chooser.showOpenDialog(null)
    val configFileReader = new ConfigurationFileReader()
    //    val config = configFileWriter.read(chooser.selectedFile)

    val testConfigPathLaptop = "/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion/test_config.xml"
    val testConfigPathDesktop = "test-data/configs/test_config.xml"
    val config = configFileReader.read(new File(testConfigPathDesktop))
    debug("Config is read successfully")
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

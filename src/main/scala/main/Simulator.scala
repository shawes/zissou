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
    /*                              val bos : HTTPSession  = new HTTPSession()
        val inpath = "www.cmar.csiro.au/thredds/dodsC/BRAN2.1/u/ocean_u_2002_10_14.nc."
        val infileTry = Try(NetcdfFile.open(inpath))
        if (!infileTry.isSuccess) {

          debug("Couldn't open the input file " + inpath)
          //sc.stop()
          System.exit(1)
        }*/

    model.run()


  } catch {
    case ex: IllegalArgumentException => new Error("Bugger")
    //case ex: Exception => System.exit(0)
  }


}

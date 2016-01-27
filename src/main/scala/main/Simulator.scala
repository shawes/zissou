package main

import java.io.File

import grizzled.slf4j.Logger
import io.ConfigurationFileReader

object Simulator extends App {

  try {


    val logger = Logger(classOf[App])
    //    var chooser = new FileChooser(new File("/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion"))
    //    chooser.multiSelectionEnabled_=(b = false)
    //    chooser.showOpenDialog(null)
    val configFileReader = new ConfigurationFileReader()
    //    val config = configFileWriter.read(chooser.selectedFile)

    val config = configFileReader.read(new File("/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion/test_config.xml"))
    val model = new CoupledBiophysicalModel(config)
    model.run()


  } catch {
    case ex: IllegalArgumentException => new Error("Bugger")
    //case ex: Exception => System.exit(0)
  }


}

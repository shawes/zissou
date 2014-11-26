package main

import java.io.File

import io.ConfigurationFileWriter

import scala.swing._

object Simulator extends App {

  //var file = JFileDataStoreChooser.showOpenFile("shp", null)
  try {
    //var params = FileChooser.showOpenFile("xml", new File("/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion"), null)
    var chooser = new FileChooser(new File("/Users/Steven/Documents/University/Phd/Modelling/Testing/Scala Conversion"))
    chooser.multiSelectionEnabled_=(b = false)
    chooser.showOpenDialog(null)
    val configFileWriter = new ConfigurationFileWriter()
    val config = configFileWriter.read(chooser.selectedFile)


    val larvalDisperser = new LarvaeDisperser(config)
    larvalDisperser.run()

  } catch {
    case ex: IllegalArgumentException => new Error("Bugger")
  }


}

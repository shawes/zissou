package main

import java.io.File

import grizzled.slf4j.Logging
import io.config.Configuration
import io.{ConfigurationFileReader, NetcdfReader}

object Simulator extends App with Logging {

  //val model = new CoupledBiophysicalModel(readConfigurationFile)
  //model.run()
  val netcdfReader = new NetcdfReader()



  private def readConfigurationFile: Configuration = {
    val configFileReader = new ConfigurationFileReader()
    val testConfigPathDesktop = args(0)
    configFileReader.read(new File(testConfigPathDesktop))
  }
}

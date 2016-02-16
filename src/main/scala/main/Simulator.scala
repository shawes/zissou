package main

import java.io.File

import grizzled.slf4j.Logging
import io.ConfigurationFileReader

object Simulator extends App with Logging {

  try {

    val configFileReader = new ConfigurationFileReader()
    val testConfigPathDesktop = args(0)

    val config = configFileReader.read(new File(testConfigPathDesktop))
    val model = new CoupledBiophysicalModel(config)

    model.run()

  } catch {
    case ex: Exception => new Error("Zissou could not recover")
  }


}

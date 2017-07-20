package main

import java.io.File

import grizzled.slf4j.Logging
import io.config.{Configuration, ConfigurationFile}


object Simulator extends App with Logging {

  readConfigurationFile match {
    case Some(config) => new CoupledBiophysicalModel(config).run()
    case None => println("Please supply a valid configuration file.")
  }

  private def readConfigurationFile: Option[Configuration] = {
    val configurationFile = new ConfigurationFile()
    if(args.nonEmpty) {
      val testConfigPathDesktop = args(0)
      val config = configurationFile.read(new File(testConfigPathDesktop))
      Some(config)
    } else {
      None
    }
  }

}

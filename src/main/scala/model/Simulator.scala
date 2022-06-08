package model

import java.io.File
import grizzled.slf4j.Logging
import io.config.{Configuration, ConfigurationFile}

object Simulator extends App with Logging {

  readConfigurationFile match {
    case Some(config) => new CoupledBiophysicalModel(config._1, config._2).run()
    case None         => error("Please supply a valid configuration file.")
  }

  private def readConfigurationFile: Option[(Configuration, String)] = {
    val configurationFile = new ConfigurationFile()
    if (args.nonEmpty) then {
      val testConfigPathDesktop = args(0)
      val config = configurationFile.read(args(0))
      val name = getConfigName(args(0))
      Some((config, name))
    } else {
      None
    }
  }

  private def getConfigName(path: String): String = {
    val fileName = path.split("/").last
    fileName.dropRight(fileName.length - fileName.lastIndexOf("."))
  }

}

package model

import java.io.File

import grizzled.slf4j.Logging
import io.config.{Configuration, ConfigurationFile}
import org.scaladebugger.api.utils.JDITools

object Simulator extends App with Logging {

  //println("JDI is available: " + JDITools.isJdiAvailable())

  // Loads the JDI from tools.jar and attempts to
  // add it to your system classloader
  //println("Loaded JDI: " + JDITools.tryLoadJdi())

  readConfigurationFile match {
    case Some(config) => new CoupledBiophysicalModel(config._1,config._2).run()
    case None => error("Please supply a valid configuration file.")
  }

  private def readConfigurationFile: Option[(Configuration,String)] = {
    val configurationFile = new ConfigurationFile()
    if(args.nonEmpty) {
      val testConfigPathDesktop = args(0)
      val config = configurationFile.read(new File(testConfigPathDesktop))
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

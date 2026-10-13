package model

import java.io.File
import grizzled.slf4j.Logging
import io.config.{Configuration, ConfigurationFile}

object Simulator extends Logging {

  private val Usage =
    "Usage: Simulator [--parallel N] config.yaml [more.yaml ...]\n" +
    "  With several configurations, up to N (default: all of them) run at once, each in a process of its own.\n"

  def main(args: Array[String]): Unit = {
    val parallelAt = args.indexOf("--parallel")
    val parallel = if (parallelAt >= 0) args.lift(parallelAt + 1).flatMap(_.toIntOption).filter(_ >= 1) else None
    val configs = if (parallelAt >= 0) args.patch(parallelAt, Nil, 2).toSeq else args.toSeq
    if (parallelAt >= 0 && parallel.isEmpty) {
      Console.err.print(Usage)
      sys.exit(2)
    } else if (configs.size > 1 || parallel.isDefined) {
      // each configuration in its own JVM: the random number generator is one per process
      if (configs.isEmpty) { Console.err.print(Usage); sys.exit(2) }
      val codes = Batch.run(configs, parallel.getOrElse(configs.size))
      if (codes.exists(_ != 0)) sys.exit(1)
    } else single(args)
  }

  private def single(args: Array[String]): Unit = {
    readConfigurationFile(args) match {
      case Some(config) => new CoupledBiophysicalModel(config._1,config._2).run()
      case None => error("Please supply a valid configuration file.")
    }
  }

  private def readConfigurationFile(args: Array[String]): Option[(Configuration,String)] = {
    val configurationFile = new ConfigurationFile()
    if(args.nonEmpty) {
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

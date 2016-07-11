package main

import grizzled.slf4j.Logging
import io.FlowFile
import io.config.ConfigMappings._
import io.config.Configuration
import physical.flow.FlowController

/**
  * Created by steve on 27/01/2016.
  */
class PhysicalModel(val config: Configuration) extends Logging {

  debug("input files directory " + config.inputFiles.flowFilePath)
  val flowDataReader = new FlowFile(config.inputFiles.flowFilePath, config.flow)
  val flowController = new FlowController(config.flow)

  def initialise(): Unit = {
    flowController.initialiseFlow(flowDataReader)
  }

  def circulate(): Unit = {
    if (flowDataReader.hasNext) {
      flowController.refresh(flowDataReader.next())
    }
  }
}
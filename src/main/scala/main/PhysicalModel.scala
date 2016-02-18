package main

import grizzled.slf4j.Logging
import io.FlowReader
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import physical.flow.FlowController

/**
  * Created by steve on 27/01/2016.
  */
class PhysicalModel(val config: Configuration, randomNumbers: RandomNumberGenerator) extends Logging {

  debug("input files directory " + config.inputFiles.flowFilePath)
  val flowDataReader = new FlowReader(config.inputFiles, config.flow)
  val flowController = new FlowController(config.flow, randomNumbers)

  def initialise(): Unit = {
    flowController.initialiseFlow(flowDataReader)
  }

  def circulate(): Unit = {
    if (flowDataReader.hasNext) {
      flowController.refresh(flowDataReader.next())
    }
  }
}
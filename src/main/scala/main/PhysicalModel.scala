package main

import grizzled.slf4j.Logger
import io.FlowReader
import io.config.ConfigMappings._
import io.config.Configuration
import maths.RandomNumberGenerator
import physical.flow.FlowController

/**
  * Created by steve on 27/01/2016.
  */
class PhysicalModel(val config: Configuration, randomNumbers: RandomNumberGenerator) {

  val logger = Logger(classOf[PhysicalModel])
  val flowDataReader = new FlowReader(config.inputFiles, config.flow.depth)
  val flowController = new FlowController(config.flow, randomNumbers)


  def initialise(): Unit = {
    flowController.initialiseFlow(flowDataReader)
    logger.debug("There are this many polygons " + flowController.flowDataQueue.head.length)
    logger.debug("There are this many days loaded " + flowController.flowDataQueue.length)
  }

  def circulate(): Unit = {
    logger.debug("Reading next flow step")
    if (flowDataReader.hasNext) {
      logger.debug("Refreshing next flow step")
      flowController.refresh(flowDataReader.next())
    }
  }

}

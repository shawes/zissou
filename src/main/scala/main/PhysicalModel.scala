package main

import grizzled.slf4j.Logging
import io.FlowFileIterator
import io.config.ConfigMappings._
import io.config.Configuration
import physical.flow.FlowController

/**
  * Created by steve on 27/01/2016.
  */
class PhysicalModel(val config: Configuration) extends Logging {

  debug("Input files directory " + config.inputFiles.flowFilePath)
  val flowFile = new FlowFileIterator(config.inputFiles.flowFilePath, config.flow)
  val flowController = new FlowController(config.flow)

  def initialise(): Unit = {
    flowController.initialise(flowFile)
  }

  def circulate(): Unit = {
    if (flowFile.hasNext) {
      flowController.refresh(flowFile.next())
    }
  }

  def close() : Unit = {
    flowFile.closeAllOpenDatasets()
  }
}

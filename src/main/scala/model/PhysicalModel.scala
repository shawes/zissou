package model

import grizzled.slf4j.Logging
import io.FlowFileIterator
import io.config.ConfigMappings._
import io.config.Configuration
import physical.flow.FlowController

class PhysicalModel(val config: Configuration) extends Logging {

  val flowFile = new FlowFileIterator(config.flow)
  val flowController = new FlowController(flowFile, config.flow)

  def circulate(): Unit = {
    flowController.updateHydrodynamicFlow(flowFile.next())
  }

  def shutdown(): Unit = {
    flowFile.shutdown()
  }
}

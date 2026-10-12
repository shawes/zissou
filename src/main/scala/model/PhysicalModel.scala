package model

import com.github.nscala_time.time.Imports.LocalDateTime
import grizzled.slf4j.Logging
import io.FlowFileIterator
import io.config.ConfigMappings._
import io.config.Configuration
import physical.flow.FlowController

class PhysicalModel(val config: Configuration) extends Logging {

  val flowFile = new FlowFileIterator(config.flow)
  val flowController = new FlowController(flowFile, config.flow)

  /** Brings the flow up to a time: the pair of daily fields the time lies between. */
  def update(time: LocalDateTime): Unit = {
    flowController.advanceTo(time)
  }

  def shutdown(): Unit = {
    flowFile.shutdown()
  }
}

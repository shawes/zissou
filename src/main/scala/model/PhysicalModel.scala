package model

import com.github.nscala_time.time.Imports.LocalDateTime
import grizzled.slf4j.Logging
import io.FlowFileIterator
import io.config.ConfigMappings._
import io.config.Configuration
import physical.flow.{Flow, FlowController}

class PhysicalModel(val config: Configuration) extends Logging {

  private val flow: Flow = config.flow
  val flowFile = new FlowFileIterator(flow)
  // finer models nested inside the flow, each read from its own files
  private val nestedFiles = flow.nests.map(nest => new FlowFileIterator(nest))
  val flowController = new FlowController(flowFile, flow, nestedFiles)

  /** Brings the flow up to a time: the pair of daily fields the time lies between. */
  def update(time: LocalDateTime): Unit = {
    flowController.advanceTo(time)
  }

  /** The seconds spent waiting for flow data since last asked. */
  def takeWaitSeconds(): Double = (flowFile +: nestedFiles).map(_.takeWaitSeconds()).sum

  def shutdown(): Unit = {
    flowFile.shutdown()
    nestedFiles.foreach(_.shutdown())
  }
}

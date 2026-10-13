package io

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController}
import physical.{GeoCoordinate, TimeStep}
import tools.UniformFlowDataset

/** u and v packed as 16-bit integers with a scale factor, as in BRAN2020 (ocean_u_2019_01.nc stores Int16 with 3.05e-4), must come out in m/s. */
class PackedFlowTest extends AnyFlatSpec {

  private val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
  private val time = new LocalDateTime(2002, 1, 5, 12, 0)
  private val point = new GeoCoordinate(-28.0, 153.5, 5.0)

  private def flowAt(packed: Option[Double]) = {
    val root = Files.createTempDirectory("zissou-packed-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1)), packed)
    val flow = new Flow(root.toString, start to start.plusDays(10), new TimeStep(2, TimeStepType.Hour), true)
    val iterator = new FlowFileIterator(flow)
    try {
      val controller = new FlowController(iterator, flow)
      controller.advanceTo(time)
      (controller.getVelocityOfCoordinate(point, time).get, controller.getVelocity(point, time).get)
    } finally iterator.shutdown()
  }

  "Flow packed in 16 bits with a scale factor" should "be in m/s, as it is when it is not packed" in {
    val (plain, plainNearest) = flowAt(None)
    for (scale <- Seq(1e-4, 3.0518509e-4)) {
      val (packed, packedNearest) = flowAt(Some(scale))
      // 16 bits at this scale hold the flow to within half a step of it
      assert(math.abs(packed.u - plain.u) <= scale, s"u was ${packed.u}, not ${plain.u}, with a scale factor of $scale")
      assert(math.abs(packed.v - plain.v) <= scale, s"v was ${packed.v}, not ${plain.v}, with a scale factor of $scale")
      assert(math.abs(packed.w - plain.w) < 1e-9, "w is not packed")
      assert(math.abs(packedNearest.u - plainNearest.u) <= scale, s"nearest u was ${packedNearest.u}")
    }
  }
}

package io

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import exceptions.CoordinateOutOfBoundsException
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController, FlowRegion}
import physical.{GeoCoordinate, TimeStep}
import tools.UniformFlowDataset

class FlowRegionTest extends AnyFlatSpec {

  // the uniform flow's grid is 153 to 154 east and 28.5 to 27.5 south, with 15 levels
  private val root = {
    val dir = Files.createTempDirectory("zissou-region-test")
    UniformFlowDataset.write(dir.toFile, Seq((2002, 1), (2002, 2)))
    dir
  }
  private val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)

  private def flow(region: FlowRegion = FlowRegion.Default, levels: Int = FlowRegion.DefaultDepthLevels) =
    new Flow(root.toString, start to start.plusDays(20), new TimeStep(2, TimeStepType.Hour), true, region, levels)

  "The depths of the levels" should "be read from the data" in {
    assert(new FlowFileIterator(flow()).depths ==
      List(2.5, 7.5, 12.5, 17.5, 22.7, 28.2, 34.2, 41.0, 48.5, 56.7, 65.7, 75.2, 85.0, 95.0, 105.0))
  }

  it should "be the first levels asked for, from the surface" in {
    assert(new FlowFileIterator(flow(levels = 4)).depths == List(2.5, 7.5, 12.5, 17.5))
    assert(new FlowFileIterator(flow(levels = 1)).depths == List(2.5))
  }

  it should "fail with a message when the data has fewer levels than are asked for" in {
    val message = intercept[IllegalArgumentException](new FlowFileIterator(flow(levels = 16))).getMessage
    assert(message == "The flow data has 15 depth levels, fewer than the 16 that flow.depthLevels asks for")
  }

  "The region read" should "be only the part of the grid asked for" in {
    val small = FlowRegion(-28.2, -27.8, 153.2, 153.8)
    val controller = new FlowController(new FlowFileIterator(flow(small)), flow(small))
    val time = new LocalDateTime(2002, 1, 5, 12, 0)
    controller.advanceTo(time)
    assert(controller.getVelocityOfCoordinate(new GeoCoordinate(-28.0, 153.5, 5.0), time).isDefined)
    // in the files, but not in the region
    intercept[CoordinateOutOfBoundsException](controller.getVelocityOfCoordinate(new GeoCoordinate(-28.0, 153.05, 5.0), time))
    intercept[CoordinateOutOfBoundsException](controller.getVelocityOfCoordinate(new GeoCoordinate(-27.55, 153.5, 5.0), time))
  }

  it should "be the part of Australia Zissou has always read when none is given" in {
    assert(FlowRegion.Default == FlowRegion(-50.0, -10.0, 142.0, 180.0) && FlowRegion.DefaultDepthLevels == 15)
  }

  it should "be refused when it has no area" in {
    intercept[IllegalArgumentException](FlowRegion(-10, -35, 150, 160))
    intercept[IllegalArgumentException](FlowRegion(-35, -10, 160, 150))
  }

  it should "contain the points inside it, edges included" in {
    val region = FlowRegion(-35, -25, 150, 160)
    assert(region.contains(-30, 155) && region.contains(-35, 150) && region.contains(-25, 160))
    assert(!region.contains(-36, 155) && !region.contains(-30, 161))
  }
}

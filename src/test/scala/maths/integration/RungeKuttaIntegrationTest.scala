package maths.integration

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.{Constants, TimeStepType}
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController}
import physical.{GeoCoordinate, TimeStep}
import tools.UniformFlowDataset

class RungeKuttaIntegrationTest extends AnyFlatSpec {

  private val epoch = new DateTime(2002, 1, 1, 0, 0, DateTimeZone.UTC).getMillis

  // the flow is the same everywhere and is a straight line in time: the day number of the time
  // less half a day (the daily fields are for noon), times a constant for each component
  private def day(time: LocalDateTime): Double =
    (time.toDateTime(DateTimeZone.UTC).getMillis - epoch) / 86400000.0 - 0.5

  "A larva carried through a flow that changes with time" should "move as far as the flow over each step takes it" in {
    val root = Files.createTempDirectory("zissou-integration-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)))
    val timeStep = new TimeStep(6, TimeStepType.Hour) // long enough that the change of flow across a step matters
    val first = new LocalDateTime(2002, 1, 5, 12, 0) // the time of the first field
    val flow = new Flow(root.toString, first.toDateTime(DateTimeZone.UTC) to first.toDateTime(DateTimeZone.UTC).plusDays(30), timeStep, false)
    val controller = new FlowController(new FlowFileIterator(flow), flow)
    val integrator = new RungeKuttaIntegration(controller, None, timeStep.totalSeconds)

    var larva = new GeoCoordinate(-28.0, 153.2, 5.0)
    var time = first
    val seconds = timeStep.totalSeconds
    for (step <- 0 until 20) { // five days, with the flow different on every step
      controller.advanceTo(time)
      val moved = integrator.integrate(larva, time, None, 1.0).getOrElse(fail(s"step $step went nowhere"))

      // the flow is a straight line in time, so its mean over the step is its value in the middle
      val middle = day(time.plusSeconds(seconds / 2))
      val eastMetres = UniformFlowDataset.U * middle * seconds
      val northMetres = UniformFlowDataset.V * middle * seconds
      val expectedLatitude = larva.latitude + northMetres / Constants.EarthsRadius * (180 / math.Pi)
      val expectedLongitude = larva.longitude + eastMetres / Constants.EarthsRadius * (180 / math.Pi) / math.cos(larva.latitude * math.Pi / 180)

      assert(math.abs(moved.latitude - expectedLatitude) < 1e-7, s"step $step: latitude ${moved.latitude}, expected $expectedLatitude")
      assert(math.abs(moved.longitude - expectedLongitude) < 1e-7, s"step $step: longitude ${moved.longitude}, expected $expectedLongitude")
      larva = moved
      time = time.plusSeconds(seconds)
    }
  }
}

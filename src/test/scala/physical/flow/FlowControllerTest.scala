package physical.flow

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}
import tools.UniformFlowDataset

class FlowControllerTest extends AnyFlatSpec {

  private val point = new GeoCoordinate(-28.0, 153.5, 5.0)
  private val epoch = new DateTime(2002, 1, 1, 0, 0, DateTimeZone.UTC).getMillis

  private def utc(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) = new DateTime(year, month, day, hour, minute, DateTimeZone.UTC)

  private def controllerStartingAt(start: DateTime): FlowController = {
    val root = Files.createTempDirectory("zissou-controller-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2), (2002, 3)))
    val flow = new Flow(root.toString, start to start.plusDays(40), new TimeStep(2, TimeStepType.Hour), true)
    new FlowController(new FlowFileIterator(flow), flow)
  }

  // The field of each day is for noon of that day, and its value is the day number, so the
  // flow at any time after the first field's is the day number of the time with a half day
  // taken off: a straight line in time, with no steps where one pair of fields hands over to the next.
  private def expectedDay(time: LocalDateTime): Double =
    (time.toDateTime(DateTimeZone.UTC).getMillis - epoch) / 86400000.0 - 0.5

  private def dayNumberOf(velocity: Option[physical.Velocity]): Double = velocity.get.u / UniformFlowDataset.U

  "The flow at a time" should "hold the first field until the time it is for, where there is nothing earlier to blend it with" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    val first = UniformFlowDataset.dayNumber(2002, 1, 5).toDouble
    for (hour <- Seq(0, 3, 6, 9, 12)) {
      val time = new LocalDateTime(2002, 1, 5, hour, 0)
      controller.advanceTo(time)
      assert(math.abs(dayNumberOf(controller.getVelocity(point, time)) - first) < 1e-6, s"at $hour:00")
    }
  }

  it should "be the day number of the time, from the first field on: the fields blended in proportion to time" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    var time = new LocalDateTime(2002, 1, 5, 12, 0)
    var checked = 0
    while (time.isBefore(new LocalDateTime(2002, 2, 22, 0, 0))) { // over the change of month
      controller.advanceTo(time)
      val expected = expectedDay(time)
      assert(math.abs(dayNumberOf(controller.getVelocity(point, time)) - expected) < 1e-5, s"nearest cell at $time")
      assert(math.abs(dayNumberOf(controller.getVelocityOfCoordinate(point, time)) - expected) < 1e-5, s"interpolated at $time")
      time = time.plusMinutes(90)
      checked += 1
    }
    assert(checked == 760, "47.5 days in steps of 90 minutes")
  }

  it should "blend every component in the same proportion" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    val time = new LocalDateTime(2002, 1, 8, 6, 0)
    controller.advanceTo(time)
    val v = controller.getVelocityOfCoordinate(point, time).get
    val day = expectedDay(time)
    assert(math.abs(v.u - UniformFlowDataset.U * day) < 1e-7 && math.abs(v.v - UniformFlowDataset.V * day) < 1e-7 && math.abs(v.w - UniformFlowDataset.W * day) < 1e-10)
  }

  it should "not jump when one pair of fields hands over to the next" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    for (day <- 6 to 12) {
      val handover = new LocalDateTime(2002, 1, day, 12, 0)
      controller.advanceTo(handover.minusSeconds(1))
      val before = dayNumberOf(controller.getVelocityOfCoordinate(point, handover.minusSeconds(1)))
      controller.advanceTo(handover)
      val after = dayNumberOf(controller.getVelocityOfCoordinate(point, handover))
      assert(after >= before && after - before < 1e-3, s"at the handover on 2002-01-$day: $before then $after")
    }
  }

  "Advancing the flow" should "move on to the next pair of fields exactly when the time reaches the later one's" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    val first = UniformFlowDataset.dayNumber(2002, 1, 5).toDouble
    controller.advanceTo(new LocalDateTime(2002, 1, 6, 11, 59, 59))
    assert(math.abs(dayNumberOf(controller.getVelocity(point)) - first) < 1e-6, "still the first pair, whose earlier field is for 5 January")
    controller.advanceTo(new LocalDateTime(2002, 1, 6, 12, 0))
    assert(math.abs(dayNumberOf(controller.getVelocity(point)) - (first + 1)) < 1e-6)
  }

  it should "catch up over several days at once" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    controller.advanceTo(new LocalDateTime(2002, 1, 12, 13, 0))
    assert(math.abs(dayNumberOf(controller.getVelocity(point)) - UniformFlowDataset.dayNumber(2002, 1, 12)) < 1e-6)
  }

  it should "stay where it is when the time has not moved on" in {
    val controller = controllerStartingAt(utc(2002, 1, 5))
    controller.advanceTo(new LocalDateTime(2002, 1, 5, 0, 0))
    controller.advanceTo(new LocalDateTime(2002, 1, 5, 0, 0))
    assert(math.abs(dayNumberOf(controller.getVelocity(point)) - UniformFlowDataset.dayNumber(2002, 1, 5)) < 1e-6)
  }
}

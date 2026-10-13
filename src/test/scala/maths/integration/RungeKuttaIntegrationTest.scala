package maths.integration

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.{Constants, TimeStepType}
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController}
import physical.{GeoCoordinate, TimeStep, Velocity}
import tools.{EddyFlowDataset, UniformFlowDataset}

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
      val moved = integrator.integrate(larva, time, None).getOrElse(fail(s"step $step went nowhere"))

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

  it should "move as far as the flow takes it when the step runs past the time of a daily field" in {
    val root = Files.createTempDirectory("zissou-integration-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)))
    val timeStep = new TimeStep(6, TimeStepType.Hour)
    // steps from 09:00, 15:00, 21:00 and 03:00: every fourth one runs past noon, into the next pair of fields
    val first = new LocalDateTime(2002, 1, 6, 9, 0)
    val flow = new Flow(root.toString, new LocalDateTime(2002, 1, 5, 0, 0).toDateTime(DateTimeZone.UTC) to first.toDateTime(DateTimeZone.UTC).plusDays(30), timeStep, false)
    val controller = new FlowController(new FlowFileIterator(flow), flow)
    val integrator = new RungeKuttaIntegration(controller, None, timeStep.totalSeconds)

    var larva = new GeoCoordinate(-28.0, 153.2, 5.0)
    var time = first
    val seconds = timeStep.totalSeconds
    for (step <- 0 until 20) {
      controller.advanceTo(time)
      val moved = integrator.integrate(larva, time, None).getOrElse(fail(s"step $step went nowhere"))
      val middle = day(time.plusSeconds(seconds / 2))
      val eastMetres = UniformFlowDataset.U * middle * seconds
      val expectedLongitude = larva.longitude + eastMetres / Constants.EarthsRadius * (180 / math.Pi) / math.cos(larva.latitude * math.Pi / 180)
      val errorMetres = (moved.longitude - expectedLongitude) * math.Pi / 180 * Constants.EarthsRadius * math.cos(larva.latitude * math.Pi / 180)
      assert(math.abs(errorMetres) < 0.01, s"step $step from $time: $errorMetres m east of where the flow takes it")
      larva = moved
      time = time.plusSeconds(seconds)
    }
  }

  // ---- round an eddy -----------------------------------------------------------

  /**
    * Carries a larva that starts 50 km east of the centre of a steady eddy for a number of days
    * and says how far from the centre it ends up, in km, and how many radians it has gone
    * round by compared with the angle the flow gives (omega times the time).
    */
  private def aroundAnEddy(omega: Double, hours: Int, days: Int): (Double, Double) = {
    val root = Files.createTempDirectory("zissou-eddy-test")
    EddyFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)), omega)
    val timeStep = new TimeStep(hours, TimeStepType.Hour)
    val first = new LocalDateTime(2002, 1, 5, 12, 0)
    val flow = new Flow(root.toString, first.toDateTime(DateTimeZone.UTC) to first.toDateTime(DateTimeZone.UTC).plusDays(40), timeStep, false)
    val controller = new FlowController(new FlowFileIterator(flow), flow)
    val integrator = new RungeKuttaIntegration(controller, None, timeStep.totalSeconds)

    val start = EddyFlowDataset.CentreLongitude +
      50000 / (Constants.EarthsRadius * math.cos(EddyFlowDataset.CentreLatitude * math.Pi / 180)) * 180 / math.Pi
    var larva = new GeoCoordinate(EddyFlowDataset.CentreLatitude, start, 5.0)
    var time = first
    for (step <- 0 until days * 24 / hours) {
      controller.advanceTo(time)
      larva = integrator.integrate(larva, time, None).getOrElse(fail(s"step $step went nowhere"))
      time = time.plusHours(hours)
    }
    val (east, north) = EddyFlowDataset.metresFromCentre(larva.longitude, larva.latitude)
    val expectedAngle = omega * days * 86400
    val angleError = math.atan2(math.sin(math.atan2(north, east) - expectedAngle), math.cos(math.atan2(north, east) - expectedAngle))
    (math.hypot(east, north) / 1000, angleError)
  }

  "A larva carried round a strong eddy" should "stay on its circle, as the exact path does, not spiral in" in {
    // 0.5 m/s at a radius of 50 km, a turn in a week; five days in two hour steps
    val (radius, angleError) = aroundAnEddy(1.0e-5, 2, 5)
    assert(math.abs(radius - 50.0) < 1.0, f"$radius%.2f km from the centre, where the exact path stays at 50")
    assert(math.abs(angleError) < 0.03, f"$angleError%.3f radians off the exact angle")
  }

  "A larva carried round a moderate eddy" should "stay on its circle for as long as a larva drifts" in {
    // 0.15 m/s at a radius of 50 km; fifteen days in two hour steps
    val (radius, angleError) = aroundAnEddy(3.0e-6, 2, 15)
    assert(math.abs(radius - 50.0) < 1.0, f"$radius%.2f km from the centre, where the exact path stays at 50")
    assert(math.abs(angleError) < 0.05, f"$angleError%.3f radians off the exact angle")
  }

  it should "come out the same with half the time step, as a scheme of this order does" in {
    val (twoHours, _) = aroundAnEddy(3.0e-6, 2, 15)
    val (hour, _) = aroundAnEddy(3.0e-6, 1, 15)
    assert(math.abs(twoHours - hour) < 0.2, f"$twoHours%.3f km with 2 hour steps, $hour%.3f km with 1 hour steps")
  }

  "A larva that swims" should "move with the flow and its own velocity added" in {
    val root = Files.createTempDirectory("zissou-swim-integration-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)))
    val timeStep = new TimeStep(6, TimeStepType.Hour)
    val first = new LocalDateTime(2002, 1, 8, 12, 0)
    val flow = new Flow(root.toString, first.toDateTime(DateTimeZone.UTC) to first.toDateTime(DateTimeZone.UTC).plusDays(10), timeStep, false)
    val controller = new FlowController(new FlowFileIterator(flow), flow)
    val integrator = new RungeKuttaIntegration(controller, None, timeStep.totalSeconds)
    val larva = new GeoCoordinate(-28.0, 153.2, 5.0)

    controller.advanceTo(first)
    val drifting = integrator.integrate(larva, first, None).get
    val swimming = integrator.integrate(larva, first, Some(new Velocity(0.1, -0.05, 0.0))).get
    val seconds = timeStep.totalSeconds
    val eastDegrees = 0.1 * seconds / Constants.EarthsRadius * (180 / math.Pi) / math.cos(larva.latitude * math.Pi / 180)
    val northDegrees = -0.05 * seconds / Constants.EarthsRadius * (180 / math.Pi)
    assert(math.abs((swimming.longitude - drifting.longitude) - eastDegrees) < 1e-6)
    assert(math.abs((swimming.latitude - drifting.latitude) - northDegrees) < 1e-6)
  }
}

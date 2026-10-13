package maths.integration

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.{Constants, TimeStepType}
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController}
import physical.{GeoCoordinate, TimeStep}
import tools.ShelfFlowDataset

/**
  * Vertical migration takes a larva to depths of its own choosing, 75 to 100 m by day, whether or not there is a sea floor
  * above them. Over a shelf there is none of the ocean model's flow below the floor, and a larva there could not move
  * at all: it stayed where it was for every step spent there, which is half of every day for a larva that goes deep by day.
  */
class SeaFloorTest extends AnyFlatSpec {

  private val first = new LocalDateTime(2002, 1, 5, 12, 0)
  private val metresPerDegree = Constants.EarthsRadius * math.Pi / 180

  private def shelf(u: Double, v: Double, temperature: Boolean = false) = {
    val root = Files.createTempDirectory("zissou-shelf-test")
    ShelfFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)), u, v, temperature)
    val timeStep = new TimeStep(2, TimeStepType.Hour)
    val start = first.toDateTime(DateTimeZone.UTC)
    val flow = new Flow(root.toString, start to start.plusDays(30), timeStep, false)
    flow.includeTemperature = temperature
    val controller = new FlowController(new FlowFileIterator(flow), flow)
    controller.advanceTo(first)
    (new RungeKuttaIntegration(controller, None, timeStep.totalSeconds), controller, timeStep.totalSeconds)
  }

  "A larva below the sea floor" should "be taken up to where there is water, and carried by the flow there" in {
    val (integrate, controller, seconds) = shelf(0.2, 0.0)
    val deep = new GeoCoordinate(-28.0, 153.4, 80.0) // the flow stops at 30 m
    assert(controller.getVelocity(deep).isEmpty, "there is no flow at 80 m")
    val moved = integrate.integrate(deep, first, None).get
    assert(moved.depth <= ShelfFlowDataset.FloorDepth, s"still at ${moved.depth} m, below the floor")
    val metres = (moved.longitude - deep.longitude) * metresPerDegree * math.cos(math.toRadians(-28.0))
    assert(math.abs(metres - 0.2 * seconds) < 1.0, f"went $metres%.1f m east in $seconds s, at 0.2 m/s")
  }

  it should "keep moving, step after step, as long as it keeps going deep" in {
    val (integrate, controller, seconds) = shelf(0.2, 0.0)
    var larva = new GeoCoordinate(-28.0, 153.2, 80.0)
    var time = first
    for (_ <- 1 to 12) {
      controller.advanceTo(time)
      larva = integrate.integrate(larva, time, None).get
      // vertical migration sends it deep again, to 90 m, before the next step
      larva = new GeoCoordinate(larva.latitude, larva.longitude, 90.0)
      time = time.plusSeconds(seconds)
    }
    val km = (larva.longitude - 153.2) * metresPerDegree * math.cos(math.toRadians(-28.0)) / 1000
    assert(math.abs(km - 0.2 * 12 * seconds / 1000) < 0.01, f"went $km%.2f km east of 17.28")
  }

  it should "stay where there is water, and be moved by the flow as always" in {
    val (integrate, _, seconds) = shelf(0.2, -0.1)
    val shallow = new GeoCoordinate(-28.0, 153.4, 10.0)
    val moved = integrate.integrate(shallow, first, None).get
    assert(math.abs(moved.depth - 10.0) < 1e-9, "not moved vertically: there is no vertical flow")
    val east = (moved.longitude - shallow.longitude) * metresPerDegree * math.cos(math.toRadians(-28.0))
    assert(math.abs(east - 0.2 * seconds) < 1.0)
  }

  "The temperature of a larva below the sea floor" should "be the temperature of the water above it" in {
    val (_, controller, _) = shelf(0.2, 0.0, temperature = true)
    assert(controller.getTemperature(new GeoCoordinate(-28.0, 153.4, 10.0), first).contains(25.0))
    // no temperature at 80 m, there being no sea there; the nearest above it is
    assert(controller.getTemperature(new GeoCoordinate(-28.0, 153.4, 80.0), first).contains(25.0))
  }
}

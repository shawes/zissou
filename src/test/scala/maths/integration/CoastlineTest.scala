package maths.integration

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.{Constants, TimeStepType}
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController}
import physical.{GeoCoordinate, TimeStep}
import tools.CoastFlowDataset

/**
  * A coast in an ocean model's data is a place the flow is missing, and a coarse model's coast is a cell or two out from
  * the real one. A larva beside it used to stop for good as soon as a step, or any of the four flow samples of the
  * step, took it onto land. It now moves up to the coast and along it.
  */
class CoastlineTest extends AnyFlatSpec {

  private val first = new LocalDateTime(2002, 1, 5, 12, 0)
  private val metresPerDegree = Constants.EarthsRadius * math.Pi / 180

  private def integrator(u: Double, v: Double, hours: Int = 2) = {
    val root = Files.createTempDirectory("zissou-coast-test")
    CoastFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)), u, v)
    val timeStep = new TimeStep(hours, TimeStepType.Hour)
    val start = first.toDateTime(DateTimeZone.UTC)
    val flow = new Flow(root.toString, start to start.plusDays(30), timeStep, false)
    val controller = new FlowController(new FlowFileIterator(flow), flow)
    (new RungeKuttaIntegration(controller, None, timeStep.totalSeconds), controller, timeStep.totalSeconds)
  }

  private def isSea(controller: FlowController, p: GeoCoordinate) = controller.getVelocity(p).isDefined

  // follows a larva for some steps, and says where it is after each
  private def follow(from: GeoCoordinate, steps: Int, u: Double, v: Double): (Seq[GeoCoordinate], FlowController) = {
    val (integrate, controller, seconds) = integrator(u, v)
    var larva = from
    var time = first
    val path = (1 to steps).map { _ =>
      controller.advanceTo(time)
      larva = integrate.integrate(larva, time, None).getOrElse(larva)
      time = time.plusSeconds(seconds)
      larva
    }
    (path, controller)
  }

  "A larva carried towards a coast and along it" should "slide along the coast, not stop where it first touches it" in {
    // 0.3 m/s towards the land and 0.2 m/s along it, to the south: 43 km south in 30 steps of 2 hours
    val start = new GeoCoordinate(-28.0, 153.45, 5.0)
    val (path, controller) = follow(start, 30, -0.3, -0.2)
    assert(path.forall(isSea(controller, _)), "never on land")
    assert(path.forall(_.longitude >= CoastFlowDataset.CoastLongitude), s"never past the coast: ${path.map(_.longitude).min}")
    val southKm = (start.latitude - path.last.latitude) * metresPerDegree / 1000
    // it first spends some steps reaching the coast, then goes on at 0.2 m/s: nearly all of the 43 km
    assert(southKm > 38, f"went $southKm%.1f km south")
    // and it is not stuck: it is still moving at the end
    assert(path(29).latitude < path(25).latitude - 0.03, "still moving south")
  }

  it should "move at the speed of the flow along the coast when it is against it" in {
    // a step from just off the coast: the flow into the land cannot be followed, the flow along it can
    val (integrate, controller, seconds) = integrator(-0.3, -0.2)
    controller.advanceTo(first)
    val start = new GeoCoordinate(-28.0, 153.355, 5.0)
    val moved = integrate.integrate(start, first, None).get
    assert(moved.longitude == start.longitude, "it did not go west")
    val metres = (start.latitude - moved.latitude) * metresPerDegree
    assert(math.abs(metres - 0.2 * seconds) < 1.0, f"went $metres%.1f m south in $seconds s, at 0.2 m/s")
  }

  "A larva carried straight at a coast" should "go as far as the coast, and no further" in {
    val start = new GeoCoordinate(-28.0, 153.40, 5.0)
    val (path, controller) = follow(start, 20, -0.3, 0.0)
    assert(path.forall(isSea(controller, _)))
    val last = path.last
    assert(last.longitude >= CoastFlowDataset.CoastLongitude && last.longitude < CoastFlowDataset.CoastLongitude + 0.003,
      s"stopped at ${last.longitude}, at the coast (153.35)")
    assert(math.abs(last.latitude - start.latitude) < 1e-9, "and along the coast not at all, there being no flow along it")
  }

  "A larva carried away from a coast" should "move as it always has" in {
    val start = new GeoCoordinate(-28.0, 153.5, 5.0)
    val (path, _) = follow(start, 10, 0.2, -0.1)
    val eastKm = (path.last.longitude - start.longitude) * metresPerDegree * math.cos(math.toRadians(-28.0)) / 1000
    assert(math.abs(eastKm - 0.2 * 10 * 7200 / 1000) < 0.5, f"$eastKm%.2f km east")
  }

  "A larva that starts on land" should "stay where it is: there is no flow to move it" in {
    val (integrate, controller, _) = integrator(-0.3, -0.2)
    controller.advanceTo(first)
    assert(integrate.integrate(new GeoCoordinate(-28.0, 153.1, 5.0), first, None).isEmpty)
  }
}

package physical.flow

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}
import tools.GriddedFlowDataset

/**
  * A finer model nested inside the main one: a main flow of u = 1 on a 0.1 degree grid, and
  * inside it a flow of u = 2 on a 0.01 degree grid that has land in its east.
  */
class NestedFlowTest extends AnyFlatSpec {

  private val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
  private val time = new LocalDateTime(2002, 1, 5, 12, 0)
  private val NestBounds = FlowRegion(-28.2, -27.8, 153.3, 153.7)

  private def write(u: Double, lons: Array[Double], lats: Array[Double], months: Seq[(Int, Int)], land: (Double, Double) => Boolean = (_, _) => false) = {
    val root = Files.createTempDirectory("zissou-nested-test")
    GriddedFlowDataset.write(root.toFile, months, lons, lats) { (variable, lon, lat, _) =>
      if (land(lon, lat)) Double.NaN else if (variable == "u") u else 0.0
    }
    root.toString
  }

  private val main = write(1.0, GriddedFlowDataset.axis(153.0, 154.0, 0.1), GriddedFlowDataset.axis(-28.5, -27.5, 0.1), Seq((2002, 1), (2002, 2)))
  private val fineLons = GriddedFlowDataset.axis(153.3, 153.7, 0.01)
  private val fineLats = GriddedFlowDataset.axis(-28.2, -27.8, 0.01)
  private val nestWithLand = write(2.0, fineLons, fineLats, Seq((2002, 1), (2002, 2)), (lon, _) => lon >= 153.6)
  private val shortNest = write(2.0, fineLons, fineLats, Seq((2002, 1)))

  private def flowOf(path: String, region: FlowRegion, step: TimeStep, nests: List[Flow] = List.empty) =
    new Flow(path, start to start.plusDays(40), step, true, region, 15, "ocean", nests)

  private def controller(nestPath: String = nestWithLand, step: TimeStep = new TimeStep(2, TimeStepType.Hour)): FlowController = {
    val nest = flowOf(nestPath, NestBounds, step)
    val parent = flowOf(main, FlowRegion.Default, step, List(nest))
    val created = new FlowController(new FlowFileIterator(parent), parent, List(new FlowFileIterator(nest)))
    created.advanceTo(time)
    created
  }

  private def at(lat: Double, lon: Double) = new GeoCoordinate(lat, lon, 5.0)

  "A position inside a nested model" should "take its flow, not the main model's" in {
    val c = controller()
    assert(math.abs(c.getVelocityOfCoordinate(at(-28.0, 153.4), time).get.u - 2.0) < 1e-6)
    assert(math.abs(c.getVelocity(at(-28.0, 153.4), time).get.u - 2.0) < 1e-6)
    assert(math.abs(c.getVelocity(at(-28.0, 153.4)).get.u - 2.0) < 1e-6)
  }

  "A position outside it" should "take the main model's flow" in {
    val c = controller()
    assert(math.abs(c.getVelocityOfCoordinate(at(-28.0, 153.8), time).get.u - 1.0) < 1e-6)
    assert(math.abs(c.getVelocityOfCoordinate(at(-27.6, 153.4), time).get.u - 1.0) < 1e-6)
  }

  "Land in a nested model" should "be land, though the main model has sea there" in {
    val c = controller()
    val coast = at(-28.0, 153.66)
    assert(c.getVelocityOfCoordinate(coast, time).isEmpty)
    assert(c.getVelocity(coast, time).isEmpty)
    assert(c.getVelocity(coast).isEmpty)
    // the main model alone has sea at the same place
    val alone = new FlowController(new FlowFileIterator(flowOf(main, FlowRegion.Default, new TimeStep(2, TimeStepType.Hour))), flowOf(main, FlowRegion.Default, new TimeStep(2, TimeStepType.Hour)))
    alone.advanceTo(time)
    assert(alone.getVelocity(coast).isDefined)
  }

  "A nested model whose files end" should "hand its area back to the main model, and the run goes on" in {
    val c = controller(shortNest)
    assert(math.abs(c.getVelocityOfCoordinate(at(-28.0, 153.4), time).get.u - 2.0) < 1e-6)
    val later = new LocalDateTime(2002, 2, 3, 12, 0)
    var t = time
    while (t.isBefore(later)) { t = t.plusHours(2); c.advanceTo(t) }
    assert(math.abs(c.getVelocityOfCoordinate(at(-28.0, 153.4), later).get.u - 1.0) < 1e-6)
  }

  "A nested model with no files for the start of the run" should "stop the run before it begins" in {
    val nest = flowOf(write(2.0, fineLons, fineLats, Seq((2002, 3))), NestBounds, new TimeStep(2, TimeStepType.Hour))
    intercept[IllegalArgumentException](new FlowFileIterator(nest))
  }
}

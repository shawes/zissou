package physical.flow

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}
import scala.util.Random
import tools.GriddedFlowDataset

/**
  * The flow between grid nodes, tested on a flow that is a plane over the grid, which
  * interpolating from the nodes round a position reproduces exactly wherever it is.
  */
class FlowInterpolationTest extends AnyFlatSpec {

  private def u(lon: Double, lat: Double) = 0.30 + 2.0 * (lon - 153.0) + 3.0 * (lat + 28.5)
  private def v(lon: Double, lat: Double) = -0.10 + 1.5 * (lon - 153.0) - 0.5 * (lat + 28.5)

  private val longitudes = GriddedFlowDataset.axis(153.0, 154.0, 0.1)
  private val ascending = GriddedFlowDataset.axis(-28.5, -27.5, 0.1)
  private val descending = GriddedFlowDataset.axis(-27.5, -28.5, -0.1)

  private val time = new LocalDateTime(2002, 1, 5, 12, 0)

  /** A controller over the plane, with the flow missing at the given nodes (land). */
  private def controller(latitudes: Array[Double], land: Set[(Double, Double)] = Set.empty): FlowController = {
    val root = Files.createTempDirectory("zissou-interpolation-test")
    GriddedFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)), longitudes, latitudes) { (variable, lon, lat, _) =>
      if (land.contains((lon, lat))) Double.NaN
      else variable match {
        case "u" => u(lon, lat)
        case "v" => v(lon, lat)
        case _   => 0.0
      }
    }
    val flow = new Flow(root.toString, time.toDateTime(DateTimeZone.UTC) to time.toDateTime(DateTimeZone.UTC).plusDays(10), new TimeStep(2, TimeStepType.Hour), false)
    val created = new FlowController(new FlowFileIterator(flow), flow)
    created.advanceTo(time)
    created
  }

  private def at(controller: FlowController, lon: Double, lat: Double) =
    controller.getVelocityOfCoordinate(new GeoCoordinate(lat, lon, 5.0), time)

  private def assertExact(controller: FlowController, lon: Double, lat: Double): Unit = {
    val found = at(controller, lon, lat).getOrElse(fail(s"no flow at ($lon, $lat)"))
    assert(math.abs(found.u - u(lon, lat)) < 1e-5 && math.abs(found.v - v(lon, lat)) < 1e-5,
      f"at ($lon%.4f, $lat%.4f) the flow is (${found.u}%.5f, ${found.v}%.5f), not (${u(lon, lat)}%.5f, ${v(lon, lat)}%.5f)")
  }

  for ((name, latitudes) <- Seq("ascending" -> ascending, "descending" -> descending)) {

    s"Interpolating the flow, with latitude $name along the grid," should "give the flow itself at every node" in {
      val flow = controller(latitudes)
      for (lon <- longitudes.slice(1, 10); lat <- latitudes.slice(1, 10)) assertExact(flow, lon, lat)
    }

    it should "give the exact flow between nodes, from every side of a cell" in {
      val flow = controller(latitudes)
      val random = new Random(1)
      for (_ <- 0 until 400) assertExact(flow, 153.15 + random.nextDouble() * 0.7, -28.35 + random.nextDouble() * 0.7)
      // and a few points either side of a node, along each axis and across both
      for ((dlon, dlat) <- Seq((0.02, 0.0), (-0.02, 0.0), (0.0, 0.03), (0.0, -0.03), (0.04, 0.04), (-0.04, -0.04), (0.04, -0.04), (-0.04, 0.04))) {
        assertExact(flow, 153.5 + dlon, -28.0 + dlat)
      }
    }

    it should "still be exact in the outer cells, where there are too few nodes for a bicubic" in {
      val flow = controller(latitudes)
      for ((lon, lat) <- Seq((153.02, -28.0), (153.97, -28.0), (153.5, -28.47), (153.5, -27.53), (153.03, -28.46), (153.96, -27.54))) {
        assertExact(flow, lon, lat)
      }
    }

    it should "give the flow of the nearest node when the position is past the outermost nodes" in {
      val flow = controller(latitudes)
      val found = at(flow, 154.03, -28.0).get
      assert(math.abs(found.u - u(154.0, -28.0)) < 1e-5 && math.abs(found.v - v(154.0, -28.0)) < 1e-5)
    }

    it should "use the nodes it can beside land, and give no flow on the land itself" in {
      val flow = controller(latitudes, land = Set((153.5, -28.0)))
      // next to the land, but with sea nodes all round the bilinear block it needs
      assertExact(flow, 153.62, -28.0)
      assertExact(flow, 153.38, -28.0)
      assertExact(flow, 153.5, -27.88)
      // in the cell of the land node itself
      assert(at(flow, 153.52, -28.01).isEmpty)
      assert(at(flow, 153.5, -28.0).isEmpty)
    }
  }
}

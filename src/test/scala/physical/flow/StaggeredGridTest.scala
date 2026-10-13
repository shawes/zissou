package physical.flow

import java.nio.file.Files
import scala.util.Random
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}
import tools.StaggeredFlowDataset._
import tools.{StaggeredFlowDataset, UniformFlowDataset}

/** u and v on one grid and w on another, half a cell and half a level away, as in BRAN2020. */
class StaggeredGridTest extends AnyFlatSpec {

  private val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
  private val time = new LocalDateTime(2002, 1, 5, 12, 0)

  private def controller(write: java.io.File => Unit): FlowController = {
    val root = Files.createTempDirectory("zissou-staggered-test")
    write(root.toFile)
    val flow = new Flow(root.toString, start to start.plusDays(10), new TimeStep(2, TimeStepType.Hour), true)
    val c = new FlowController(new FlowFileIterator(flow), flow)
    c.advanceTo(time)
    c
  }
  private lazy val staggered = controller(StaggeredFlowDataset.write(_, Seq((2002, 1))))
  private def at(lat: Double, lon: Double, depth: Double) = new GeoCoordinate(lat, lon, depth)

  "The depths of the levels" should "be read from each variable's own axis, w's being half a level from u's" in {
    val dir = Files.createTempDirectory("zissou-staggered-depths")
    StaggeredFlowDataset.write(dir.toFile, Seq((2002, 1)))
    val iterator = new FlowFileIterator(new Flow(dir.toString, start to start.plusDays(10), new TimeStep(2, TimeStepType.Hour), true))
    try {
      assert(iterator.current.componentDepths == List(Levels.toList, Levels.toList, Interfaces.toList))
      assert(iterator.depths == Levels.toList)
    } finally iterator.shutdown()
  }

  "Flow on grids of their own" should "be interpolated from each variable's own nodes, at positions all over the cells" in {
    val random = new Random(7)
    var checked = 0
    for (_ <- 1 to 300) {
      // inside the nodes of both grids, so that every variable has a full block of nodes round it
      val lon = 153.25 + random.nextDouble() * 0.5
      val lat = -28.25 + random.nextDouble() * 0.5
      val depth = 40.0 + random.nextDouble() * 20 // nearest levels of u, v and w are different ones
      val flow = staggered.getVelocityOfCoordinate(at(lat, lon, depth), time).get
      val uLevel = Levels.minBy(l => math.abs(l - depth))
      val wLevel = Interfaces.minBy(l => math.abs(l - depth))
      assert(math.abs(flow.u - u(lon, lat, uLevel)) < 1e-5, s"u at ($lat, $lon, $depth): ${flow.u}")
      assert(math.abs(flow.v - v(lon, lat, uLevel)) < 1e-5, s"v at ($lat, $lon, $depth): ${flow.v}")
      assert(math.abs(flow.w - w(lon, lat, wLevel)) < 1e-7, s"w at ($lat, $lon, $depth): ${flow.w}")
      checked += 1
    }
    assert(checked == 300)
  }

  it should "give w from w's own level for a depth between u's levels" in {
    // 7.4 m: the nearest level of u and v is 7.5 m, and the nearest of w is its 5 m, not its 10 m
    val flow = staggered.getVelocityOfCoordinate(at(-28.0, 153.5, 7.4), time).get
    assert(math.abs(flow.u - u(153.5, -28.0, 7.5)) < 1e-5)
    assert(math.abs(flow.w - w(153.5, -28.0, 5.0)) < 1e-7, s"w was ${flow.w}")
  }

  it should "be read at the nearest node of each variable's own grid, when asked for the nearest cell" in {
    // 153.34 east, 28.04 south: the nearest u node is 153.3, -28.0; the nearest w node is 153.35, -28.05
    val flow = staggered.getVelocity(at(-28.04, 153.34, 12.4), time).get
    assert(math.abs(flow.u - u(153.3, -28.0, 12.5)) < 1e-5, s"u ${flow.u}")
    assert(math.abs(flow.v - v(153.3, -28.0, 12.5)) < 1e-5)
    assert(math.abs(flow.w - w(153.35, -28.05, 10.0)) < 1e-7, s"w ${flow.w}")
    assert(staggered.getVelocity(at(-28.04, 153.34, 12.4)).isDefined)
  }

  it should "use the nearest node of w at the edge of the grid, where w has no block of nodes round it" in {
    // 153.01 east is inside the nodes of u but before the first node of w (153.05): w holds its edge value
    val flow = staggered.getVelocityOfCoordinate(at(-28.0, 153.01, 5.0), time).get
    assert(math.abs(flow.w - w(153.05, -28.05, 5.0)) < 1e-7 || math.abs(flow.w - w(153.05, -27.95, 5.0)) < 1e-7, s"w ${flow.w}")
    assert(math.abs(flow.u - u(153.01, -28.0, 2.5)) < 1e-5 || math.abs(flow.u - u(153.01, -28.0, 7.5)) < 1e-5)
  }

  "Flow on one grid" should "not be taken for flow on grids of their own" in {
    val uniform = controller(UniformFlowDataset.write(_, Seq((2002, 1))))
    val iterator = new FlowFileIterator(new Flow(
      { val dir = Files.createTempDirectory("zissou-colocated"); UniformFlowDataset.write(dir.toFile, Seq((2002, 1))); dir.toString },
      start to start.plusDays(10), new TimeStep(2, TimeStepType.Hour), true))
    try assert(iterator.current.colocated) finally iterator.shutdown()
    assert(uniform.getVelocityOfCoordinate(at(-28.0, 153.5, 5.0), time).isDefined)
    val dir = Files.createTempDirectory("zissou-staggered-flag")
    StaggeredFlowDataset.write(dir.toFile, Seq((2002, 1)))
    val other = new FlowFileIterator(new Flow(dir.toString, start to start.plusDays(10), new TimeStep(2, TimeStepType.Hour), true))
    try assert(!other.current.colocated) finally other.shutdown()
  }
}

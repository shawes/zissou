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

  // fields that curve: bicubic interpolation reproduces them among the 4 by 4 nodes round a position,
  // and bilinear does not, so which one is used shows
  private val Curvature = 5.0
  private lazy val curved = controller(StaggeredFlowDataset.write(_, Seq((2002, 1)), Curvature))

  it should "interpolate bicubically where the 4 by 4 nodes are there, and bilinearly in the outermost gaps" in {
    def error(lon: Double, lat: Double) = {
      val flow = curved.getVelocityOfCoordinate(at(lat, lon, 2.5), time).get
      (math.abs(flow.u - (u(lon, lat, 2.5) + curve(lon, lat, Curvature))), math.abs(flow.v - (v(lon, lat, 2.5) + curve(lon, lat, Curvature))))
    }
    // u and v's nodes are 153.0, 153.1 ... 154.0 east and 28.5, 28.4 ... 27.5 south: gaps from the first to the last
    for (lon <- Seq(153.15, 153.35, 153.5, 153.75, 153.85); lat <- Seq(-28.15, -28.0, -27.85)) {
      val (eu, ev) = error(lon, lat)
      assert(eu < 1e-5 && ev < 1e-5, s"inside the 4 by 4 nodes at ($lat, $lon): u off by $eu, v by $ev")
    }
    // the gaps at the edges have no node beyond them on one side: bilinear, which is off by curvature * h^2 * x(1 - x)
    for (lon <- Seq(153.05, 153.95)) {
      val (eu, _) = error(lon, -28.0)
      assert(eu > 5e-3, s"in the outermost gap at $lon: u off by only $eu")
    }
    for (lat <- Seq(-28.45, -27.55)) {
      val (eu, _) = error(153.5, lat)
      assert(eu > 5e-3, s"in the outermost gap at $lat: u off by only $eu")
    }
  }

  it should "do the same for w, whose nodes are half a cell inside" in {
    def error(lon: Double, lat: Double) =
      math.abs(curved.getVelocityOfCoordinate(at(lat, lon, 5.0), time).get.w - (w(lon, lat, 5.0) + curve(lon, lat, Curvature)))
    // w's nodes are 153.05, 153.15 ... 154.05 east and 28.45 ... 27.45 south: 11 of each, half a cell from u's
    for (lon <- Seq(153.25, 153.5, 153.75); lat <- Seq(-28.25, -28.0, -27.75)) assert(error(lon, lat) < 1e-6, s"inside at ($lat, $lon)")
    assert(error(153.1, -28.0) > 5e-3 && error(153.99, -28.0) > 5e-3, "in w's outermost gaps")
  }
}

package physical.flow

import java.nio.file.Files
import scala.collection.parallel.CollectionConverters._
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}
import tools.UniformFlowDataset

class TemperatureFlowTest extends AnyFlatSpec {

  private val epoch = new DateTime(2002, 1, 1, 0, 0, DateTimeZone.UTC).getMillis
  private val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)

  // a plane in longitude, latitude and day, which interpolating among the nodes round a position reproduces exactly
  private def temperature(lon: Double, lat: Double, day: Double) = 15.0 + 4.0 * (lon - 153.0) + 2.0 * (lat + 28.5) + 0.1 * day

  // the daily fields are for noon, and the flow at a time is a straight line between them, so a field is the day number of the time less half a day
  private def dayAt(time: LocalDateTime) = (time.toDateTime(DateTimeZone.UTC).getMillis - epoch) / 86400000.0 - 0.5

  private def controller(withTemperature: Boolean, wanted: Boolean = true): FlowController = {
    val root = Files.createTempDirectory("zissou-temperature-test")
    if (withTemperature) UniformFlowDataset.writeWithTemperature(root.toFile, Seq((2002, 1), (2002, 2)), (lon, lat, day) => temperature(lon, lat, day.toDouble))
    else UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)))
    val flow = new Flow(root.toString, start to start.plusDays(30), new TimeStep(2, TimeStepType.Hour), true)
    flow.includeTemperature = wanted
    new FlowController(new FlowFileIterator(flow), flow)
  }

  "The temperature" should "be interpolated among the nodes round a position, and blended in time like the flow" in {
    val c = controller(withTemperature = true)
    var checked = 0
    for (day <- 6 to 12; hour <- Seq(0, 5, 12, 17); (lat, lon, depth) <- Seq((-28.0, 153.5, 5.0), (-28.23, 153.37, 60.0), (-27.71, 153.83, 100.0))) {
      val time = new LocalDateTime(2002, 1, day, hour, 0)
      c.advanceTo(time)
      val expected = temperature(lon, lat, dayAt(time))
      val actual = c.getTemperature(new GeoCoordinate(lat, lon, depth), time).get
      assert(math.abs(actual - expected) < 1e-4, s"at $time ($lat, $lon): $actual, not $expected")
      checked += 1
    }
    assert(checked == 84)
  }

  it should "leave the flow as it was" in {
    val withTemperature = controller(withTemperature = true)
    val without = controller(withTemperature = false, wanted = false)
    val time = new LocalDateTime(2002, 1, 8, 7, 0)
    withTemperature.advanceTo(time); without.advanceTo(time)
    val p = new GeoCoordinate(-28.0, 153.5, 5.0)
    val a = withTemperature.getVelocityOfCoordinate(p, time).get
    val b = without.getVelocityOfCoordinate(p, time).get
    assert(a.u == b.u && a.v == b.v && a.w == b.w)
    assert(withTemperature.getVelocity(p).isDefined && withTemperature.getVelocity(p, time).isDefined)
  }

  it should "be nothing when it was not asked for, even if the files are there" in {
    val c = controller(withTemperature = true, wanted = false)
    val time = new LocalDateTime(2002, 1, 8, 7, 0)
    c.advanceTo(time)
    assert(c.getTemperature(new GeoCoordinate(-28.0, 153.5, 5.0), time).isEmpty)
  }

  it should "be asked of many larvae at once, as the model does its mortality" in {
    val c = controller(withTemperature = true)
    val time = new LocalDateTime(2002, 1, 9, 3, 0)
    c.advanceTo(time)
    val results = (0 until 4000).par.map { i =>
      val lat = -28.4 + (i % 80) * 0.0105
      val lon = 153.1 + (i / 80 % 50) * 0.016
      math.abs(c.getTemperature(new GeoCoordinate(lat, lon, 5.0 + (i % 7) * 10), time).get - temperature(lon, lat, dayAt(time)))
    }
    assert(results.max < 1e-4)
  }

  it should "stop the run, saying which files are missing, when it is asked for and there are none" in {
    val root = Files.createTempDirectory("zissou-no-temperature-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)))
    val flow = new Flow(root.toString, start to start.plusDays(30), new TimeStep(2, TimeStepType.Hour), true)
    flow.includeTemperature = true
    val problem = intercept[IllegalArgumentException](new FlowFileIterator(flow))
    assert(problem.getMessage.contains("temp flow file") && problem.getMessage.contains("2002-01"), problem.getMessage)
  }
}

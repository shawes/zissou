package io

import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController, FlowRegion}
import physical.{GeoCoordinate, TimeStep}

/**
  * Reads CSIRO's BRAN2020 daily flow from NCI's THREDDS server. It needs the network, so it only
  * runs when ZISSOU_LIVE_TESTS is set:
  *
  *   ZISSOU_LIVE_TESTS=1 sbt "testOnly io.Bran2020LiveTest"
  */
class Bran2020LiveTest extends AnyFlatSpec {

  private val address = "https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily"
  private def live = sys.env.contains("ZISSOU_LIVE_TESTS")
  private def utc(year: Int, month: Int, day: Int) = new DateTime(year, month, day, 0, 0, DateTimeZone.UTC)

  private def flow(start: DateTime, days: Int, region: FlowRegion = FlowRegion(-30, -26, 152, 156), levels: Int = 5) =
    new Flow(address, start to start.plusDays(days), new TimeStep(2, TimeStepType.Hour), true, region, levels)

  "BRAN2020 over OPeNDAP" should "be read across a change of month, with the day after read ahead" in {
    assume(live, "set ZISSOU_LIVE_TESTS to run the tests that need the network")
    val config = flow(utc(2019, 1, 30), 5)
    val iterator = new FlowFileIterator(config)
    try {
      assert(iterator.depths.size == 5 && iterator.depths.head == 2.5)
      // u and v on the corners of cells and the centres of layers, w on the centres of cells and the bottoms of layers
      assert(iterator.current.componentDepths.map(_.head) == List(2.5, 2.5, 5.0))
      val controller = new FlowController(iterator, config)
      val stamps = scala.collection.mutable.ArrayBuffer(iterator.current.laterStamp)
      // 30 January to 3 February: over the change of month
      for (_ <- 1 to 4) stamps += iterator.next().laterStamp
      assert(stamps.map(_.toString("yyyy-MM-dd HH:mm")) == Seq(
        "2019-01-31 12:00", "2019-02-01 12:00", "2019-02-02 12:00", "2019-02-03 12:00", "2019-02-04 12:00"))
      // the flow off the Queensland coast is real: finite, and a few tens of cm/s at most
      val time = new LocalDateTime(2019, 2, 3, 12, 0)
      controller.advanceTo(time)
      val velocity = controller.getVelocityOfCoordinate(new GeoCoordinate(-28.0, 154.5, 5.0), time).get
      assert(velocity.isDefined && math.hypot(velocity.u, velocity.v) < 3.0, velocity.toString)
    } finally iterator.shutdown()
  }

  it should "have the strength of flow the East Australian Current has, in m/s" in {
    assume(live, "set ZISSOU_LIVE_TESTS to run the tests that need the network")
    // u and v are stored in 16 bits with a scale factor; scaled twice they came out 3,000 times too small
    val config = flow(utc(2019, 11, 1), 3, FlowRegion(-36, -28, 150, 156), 3)
    val iterator = new FlowFileIterator(config)
    try {
      val controller = new FlowController(iterator, config)
      val time = new LocalDateTime(2019, 11, 1, 12, 0)
      controller.advanceTo(time)
      val speeds = for (lat <- (0 to 12).map(-35.0 + _ * 0.5); lon <- (0 to 10).map(150.5 + _ * 0.5))
        yield controller.getVelocityOfCoordinate(new GeoCoordinate(lat, lon, 5.0), time).map(v => math.hypot(v.u, v.v))
      val open = speeds.flatten
      assert(open.size > 50, "most of the sea off NSW has flow")
      assert(open.max > 0.3 && open.max < 3.0, s"the fastest of ${open.size} points was ${open.max} m/s")
      assert(open.sum / open.size > 0.05, s"the mean was ${open.sum / open.size} m/s")
    } finally iterator.shutdown()
  }

  it should "say which month has no data when the run starts before it begins" in {
    assume(live, "set ZISSOU_LIVE_TESTS to run the tests that need the network")
    val problem = intercept[IllegalArgumentException](new FlowFileIterator(flow(utc(1992, 12, 5), 5)))
    assert(problem.getMessage.contains("1992-12") && problem.getMessage.contains(address), problem.getMessage)
  }

  it should "stop with a message at the end of the data" in {
    assume(live, "set ZISSOU_LIVE_TESTS to run the tests that need the network")
    val iterator = new FlowFileIterator(flow(utc(2023, 12, 29), 10))
    try {
      iterator.next() // 30 and 31 December
      val problem = intercept[FlowDataExhaustedException](iterator.next())
      assert(problem.getMessage.contains("2023-12-31"), problem.getMessage)
    } finally iterator.shutdown()
  }
}

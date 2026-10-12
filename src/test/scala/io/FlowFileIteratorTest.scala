package io

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowGridWrapper}
import physical.{GeoCoordinate, TimeStep}
import tools.UniformFlowDataset

class FlowFileIteratorTest extends AnyFlatSpec {

  private val point = new GeoCoordinate(-28.0, 153.5, 5.0)

  private def iterator(start: DateTime, months: Seq[(Int, Int)]): FlowFileIterator = {
    val root = Files.createTempDirectory("zissou-iterator-test")
    UniformFlowDataset.write(root.toFile, months)
    new FlowFileIterator(new Flow(root.toString, start to start.plusDays(80), new TimeStep(2, TimeStepType.Hour), true))
  }

  private def utc(year: Int, month: Int, day: Int, hour: Int = 0) = new DateTime(year, month, day, hour, 0, DateTimeZone.UTC)

  /** The day numbers of the earlier and the later field a window holds. */
  private def days(window: FlowGridWrapper): (Long, Long) = {
    val index = window.getIndex(point)
    def dayOf(isEarlier: Boolean) = math.round(window.getVelocity((index._1, index._2, index._3, isEarlier)).get.u / UniformFlowDataset.U)
    (dayOf(true), dayOf(false))
  }

  "The first window" should "hold the day the run starts on and the day after it" in {
    val window = iterator(utc(2002, 1, 5), Seq((2002, 1), (2002, 2), (2002, 3))).current
    assert(days(window) == (UniformFlowDataset.dayNumber(2002, 1, 5), UniformFlowDataset.dayNumber(2002, 1, 6)))
  }

  it should "be left alone by asking for it" in {
    val flow = iterator(utc(2002, 1, 5), Seq((2002, 1), (2002, 2), (2002, 3)))
    assert(days(flow.current) == days(flow.current))
  }

  it should "carry the times of its two fields, which the data stamps at noon" in {
    val window = iterator(utc(2002, 1, 5), Seq((2002, 1), (2002, 2), (2002, 3))).current
    assert(window.earlierStamp == new LocalDateTime(2002, 1, 5, 12, 0))
    assert(window.laterStamp == new LocalDateTime(2002, 1, 6, 12, 0))
  }

  "Moving on" should "go one day at a time, from one month's file into the next" in {
    val flow = iterator(utc(2002, 1, 25), Seq((2002, 1), (2002, 2), (2002, 3)))
    val first = UniformFlowDataset.dayNumber(2002, 1, 25)
    assert(days(flow.current) == (first, first + 1))
    for (k <- 1 to 28) { // from 25 January to 22 February, over the change of month
      val window = flow.next()
      assert(days(window) == (first + k, first + k + 1), s"after $k days")
      assert(window.earlierStamp == new LocalDateTime(2002, 1, 25, 12, 0).plusDays(k), s"after $k days")
      assert(window.laterStamp == window.earlierStamp.plusDays(1), s"after $k days")
    }
  }
}

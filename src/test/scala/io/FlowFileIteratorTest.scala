package io

import java.nio.file.Files
import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
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

  "A run that starts on the last day of a month" should "carry on into the next month's file" in {
    val window = iterator(utc(2002, 1, 31), Seq((2002, 1), (2002, 2), (2002, 3))).current
    assert(days(window) == (UniformFlowDataset.dayNumber(2002, 1, 31), UniformFlowDataset.dayNumber(2002, 2, 1)))
    assert(window.laterStamp == new LocalDateTime(2002, 2, 1, 12, 0))
  }

  "Reading the files" should "use every file in the folder, the last one too, month after month" in {
    val flow = iterator(utc(2002, 2, 20), Seq((2002, 1), (2002, 2), (2002, 3)))
    val first = UniformFlowDataset.dayNumber(2002, 2, 20)
    assert(days(flow.current) == (first, first + 1))
    // 20 February to the last pair, 30 and 31 March: 38 days, over two changes of month
    for (k <- 1 to 38) {
      assert(days(flow.next()) == (first + k, first + k + 1), s"after $k days")
    }
    assert(days(flow.current) == (UniformFlowDataset.dayNumber(2002, 3, 30), UniformFlowDataset.dayNumber(2002, 3, 31)))
  }

  it should "say when the files run out, not start the month again" in {
    val flow = iterator(utc(2002, 3, 29), Seq((2002, 1), (2002, 2), (2002, 3)))
    flow.next() // 30 and 31 March: the last pair there is
    val last = days(flow.current)
    val problem = intercept[FlowDataExhaustedException](flow.next())
    assert(problem.getMessage.contains("2002-03-31"), problem.getMessage)
    assert(days(flow.current) == last, "the window is as it was")
    intercept[FlowDataExhaustedException](flow.next()) // and it says so again
  }

  it should "say so at the start when the run begins on the last day there is data for" in {
    val problem = intercept[FlowDataExhaustedException](iterator(utc(2002, 3, 31), Seq((2002, 1), (2002, 2), (2002, 3))))
    assert(problem.getMessage.contains("2002-03-31"))
  }

  it should "refuse a gap in the months, not carry on with the wrong file" in {
    val flow = iterator(utc(2002, 1, 29), Seq((2002, 1), (2002, 3), (2002, 4)))
    flow.next() // 30 and 31 January
    val problem = intercept[IllegalStateException](flow.next())
    assert(problem.getMessage.contains("2002-02"), problem.getMessage)
  }

  it should "ignore files in the folders that are not named like a month's flow" in {
    val root = Files.createTempDirectory("zissou-iterator-test")
    UniformFlowDataset.write(root.toFile, Seq((2002, 1), (2002, 2)))
    for (variable <- Seq("u", "v", "w"); stray <- Seq("notes.nc", "ocean_u_readme.nc", "ocean_u_2002_13.nc")) {
      Files.write(root.resolve(variable).resolve(stray), Array[Byte](1, 2, 3))
    }
    val source = new LocalFlowSource(root.toString, List("u", "v", "w"))
    assert(source.has(java.time.YearMonth.of(2002, 1)) && source.has(java.time.YearMonth.of(2002, 2)))
    assert(source.firstAfter(java.time.YearMonth.of(2002, 1)).contains(java.time.YearMonth.of(2002, 2)))
    assert(source.firstAfter(java.time.YearMonth.of(2002, 2)).isEmpty)
    val start = utc(2002, 1, 5)
    val flow = new FlowFileIterator(new Flow(root.toString, start to start.plusDays(20), new TimeStep(2, TimeStepType.Hour), true), source)
    assert(days(flow.current) == (UniformFlowDataset.dayNumber(2002, 1, 5), UniformFlowDataset.dayNumber(2002, 1, 6)))
  }

  it should "say which month has no file when the run starts in one" in {
    val problem = intercept[IllegalArgumentException](iterator(utc(2002, 5, 1), Seq((2002, 1), (2002, 2), (2002, 3))))
    assert(problem.getMessage.contains("2002-05"), problem.getMessage)
  }
}

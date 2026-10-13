package io

import java.nio.file.Files
import java.time.YearMonth
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import exceptions.FlowDataExhaustedException
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowGridWrapper}
import physical.TimeStep
import tools.UniformFlowDataset
import ucar.nc2.dt.grid.GridDataset

/** Reading the flow from a slow source: a day ahead on a thread of its own, with the same days in the same order. */
class FlowReadAheadTest extends AnyFlatSpec {

  /** A folder of files that behaves as a remote source does: slow, and read by whichever thread asks. */
  private class SlowSource(inner: FlowSource, delayMillis: Long = 0) extends FlowSource {
    val readsBy = new ConcurrentLinkedQueue[String]()
    @volatile var failOpeningFrom: Option[YearMonth] = None
    override def isRemote: Boolean = true
    def has(month: YearMonth): Boolean = inner.has(month)
    def firstAfter(month: YearMonth): Option[YearMonth] = inner.firstAfter(month)
    def open(variable: String, month: YearMonth): GridDataset = {
      if (failOpeningFrom.exists(!month.isBefore(_))) throw new java.io.IOException(s"the server dropped $variable $month")
      inner.open(variable, month)
    }
    def description: String = inner.description
    override def retrying[T](what: String)(body: => T): T = {
      if (delayMillis > 0) Thread.sleep(delayMillis)
      readsBy.add(Thread.currentThread.getName + ": " + what)
      body
    }
  }

  private val Months = Seq((2002, 1), (2002, 2), (2002, 3))
  private val root = {
    val dir = Files.createTempDirectory("zissou-readahead-test")
    UniformFlowDataset.write(dir.toFile, Months)
    dir
  }
  private def flow(start: DateTime) = new Flow(root.toString, start to start.plusDays(80), new TimeStep(2, TimeStepType.Hour), true)
  private def utc(year: Int, month: Int, day: Int) = new DateTime(year, month, day, 0, 0, DateTimeZone.UTC)
  private def local = new LocalFlowSource(root.toString, FlowFileIterator.Variables)

  private def dayNumbers(window: FlowGridWrapper): (Long, Long) = {
    val index = window.getIndex(new physical.GeoCoordinate(-28.0, 153.5, 5.0))
    def dayOf(isEarlier: Boolean) = math.round(window.getVelocity((index._1, index._2, index._3, isEarlier)).get.u / UniformFlowDataset.U)
    (dayOf(true), dayOf(false))
  }

  private def readerThreads = Thread.getAllStackTraces.keySet.asScala.count(t => t.getName == "zissou-flow-reader" && t.isAlive)

  private def waitFor(what: String)(condition: => Boolean): Unit = {
    val deadline = System.currentTimeMillis + 10000
    while (!condition && System.currentTimeMillis < deadline) Thread.sleep(10)
    assert(condition, s"timed out waiting for $what")
  }

  "A slow source" should "give the same days in the same order as reading each day when it is wanted" in {
    val plain = new FlowFileIterator(flow(utc(2002, 2, 20)), local)
    val ahead = new FlowFileIterator(flow(utc(2002, 2, 20)), new SlowSource(local))
    try {
      assert(dayNumbers(ahead.current) == dayNumbers(plain.current))
      // 20 February to 30 and 31 March: 38 days, over a change of month
      for (k <- 1 to 38) {
        val wanted = dayNumbers(plain.next())
        val got = ahead.next()
        assert(dayNumbers(got) == wanted, s"after $k days")
        assert(got.laterStamp == plain.current.laterStamp)
      }
    } finally { plain.shutdown(); ahead.shutdown() }
  }

  it should "read the day after the next on a thread of its own, before it is wanted" in {
    val source = new SlowSource(local, delayMillis = 20)
    val iterator = new FlowFileIterator(flow(utc(2002, 1, 5)), source)
    try {
      // at the start the first two days are read where the iterator is made, and a third in the background
      waitFor("the third day to be read ahead")(source.readsBy.asScala.exists(_.startsWith("zissou-flow-reader")))
      val readAhead = source.readsBy.asScala.filter(_.startsWith("zissou-flow-reader")).toSeq
      assert(readAhead.size == 1 && readAhead.head.contains("reading day 7"), readAhead.toString)
      assert(source.readsBy.asScala.filterNot(_.startsWith("zissou-flow-reader")).size == 2)
      // and each time it moves on, one more
      iterator.next()
      waitFor("the next day to be read ahead")(source.readsBy.asScala.count(_.startsWith("zissou-flow-reader")) == 2)
    } finally iterator.shutdown()
  }

  it should "not take the end of a month for the end of the files when it has already opened the next month" in {
    val source = new SlowSource(local)
    val iterator = new FlowFileIterator(flow(utc(2002, 2, 27)), source)
    try {
      // 27 and 28 February are held; the reader opens March for 1 March
      waitFor("1 March to be read ahead")(source.readsBy.asScala.exists(_.startsWith("zissou-flow-reader: reading day 1 of 2002-03")))
      val window = iterator.next()
      assert(dayNumbers(window)._2 == UniformFlowDataset.dayNumber(2002, 3, 1))
      assert(dayNumbers(iterator.next())._2 == UniformFlowDataset.dayNumber(2002, 3, 2))
    } finally iterator.shutdown()
  }

  it should "not read ahead of the last day there is data for" in {
    // the last day there is data for is the last day of March
    val source = new SlowSource(local)
    val iterator = new FlowFileIterator(flow(utc(2002, 3, 29)), source)
    try {
      waitFor("31 March to be read ahead")(source.readsBy.asScala.exists(_.startsWith("zissou-flow-reader")))
      iterator.next() // 30 and 31 March
      Thread.sleep(200)
      intercept[FlowDataExhaustedException](iterator.next())
      val readAhead = source.readsBy.asScala.filter(_.startsWith("zissou-flow-reader")).toSeq
      assert(readAhead.size == 1 && readAhead.head.contains("reading day 31"), readAhead.toString)
    } finally iterator.shutdown()
  }

  it should "say when the files run out, as reading each day does, with the window as it was" in {
    val iterator = new FlowFileIterator(flow(utc(2002, 3, 28)), new SlowSource(local))
    try {
      iterator.next() // 29 and 30 March
      iterator.next() // 30 and 31 March
      val last = dayNumbers(iterator.current)
      val problem = intercept[FlowDataExhaustedException](iterator.next())
      assert(problem.getMessage.contains("2002-03-31"), problem.getMessage)
      assert(dayNumbers(iterator.current) == last)
    } finally iterator.shutdown()
  }

  it should "fail with what went wrong in a read ahead, when the day it was reading is wanted" in {
    val source = new SlowSource(local)
    val iterator = new FlowFileIterator(flow(utc(2002, 1, 29)), source)
    try {
      source.failOpeningFrom = Some(YearMonth.of(2002, 2)) // 30 January is read; 31 January and then February are not there to open
      iterator.next() // 30 and 31 January: both already read
      val problem = intercept[java.io.IOException](iterator.next())
      assert(problem.getMessage.contains("2002-02"), problem.getMessage)
    } finally iterator.shutdown()
  }

  it should "stop its thread when the iterator is shut down" in {
    val before = readerThreads
    val iterator = new FlowFileIterator(flow(utc(2002, 1, 5)), new SlowSource(local, delayMillis = 50))
    assert(readerThreads == before + 1)
    iterator.shutdown()
    waitFor("the reader thread to stop")(readerThreads == before)
  }

  "The time spent waiting for the flow" should "be counted when a day is not ready, and not when it is" in {
    val source = new SlowSource(local, delayMillis = 600)
    val iterator = new FlowFileIterator(flow(utc(2002, 1, 5)), source)
    try {
      iterator.takeWaitSeconds() // what making it took
      waitFor("the day ahead to be read")(source.readsBy.asScala.exists(_.startsWith("zissou-flow-reader")))
      Thread.sleep(300) // so the day ahead is finished
      iterator.next()
      assert(iterator.takeWaitSeconds() < 0.3, "the day was ready")
      iterator.next() // asked for at once: the one being read ahead is not done
      val waited = iterator.takeWaitSeconds()
      assert(waited > 0.3 && waited < 2.0, s"waited $waited secs for a day that takes 0.6 secs")
      assert(iterator.takeWaitSeconds() == 0.0, "asking starts the count again")
    } finally iterator.shutdown()
  }

  it should "be reported again and again for as long as a day is being waited for" in {
    val reports = new ConcurrentLinkedQueue[(String, Long)]()
    val source = new SlowSource(local, delayMillis = 2500)
    val iterator = new FlowFileIterator(flow(utc(2002, 1, 5)), source, reportAfterSeconds = 1) {
      override protected def reportWaiting(wanted: org.joda.time.LocalDate, seconds: Long): Unit = reports.add((wanted.toString, seconds))
    }
    try {
      iterator.next() // the day after the next was begun when this one was made, and is being read for 2.5 secs
      iterator.next()
      val seen = reports.asScala.toSeq
      // each of the two days waited for is reported at 1 and 2 seconds, and by the date it is for
      val byDay = seen.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
      assert(byDay.keySet == Set("2002-01-07", "2002-01-08"), s"reported $seen")
      assert(byDay.values.forall(_ == Seq(1L, 2L)), s"reported $seen")
    } finally iterator.shutdown()
  }

  "A source that is not slow" should "be read where and when it is wanted, with no thread of its own" in {
    val before = readerThreads
    val iterator = new FlowFileIterator(flow(utc(2002, 1, 5)), local)
    try {
      iterator.next()
      assert(readerThreads == before)
    } finally iterator.shutdown()
  }
}

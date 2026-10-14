package ui

import org.scalatest.flatspec.AnyFlatSpec

class RunProgressTest extends AnyFlatSpec {

  private val log =
    """WARNING: something from the JVM
      |[main] INFO model.CoupledBiophysicalModel - Simulation run started
      |[main] INFO model.CoupledBiophysicalModel - Day 2002-01-05 has been completed in 1 secs with 231 larvae.
      |[main] INFO model.CoupledBiophysicalModel - Day 2002-01-06 has been completed in 0 secs with 176 larvae.
      |[main] WARN model.CoupledBiophysicalModel - 654 larvae left the edge of the flow grid and were killed
      |[main] INFO model.CoupledBiophysicalModel - Simulation run completed in 0.02 minutes
      |""".stripMargin

  "The log summary" should "list each completed day" in {
    val summary = RunProgress.parse(log)
    assert(summary.days == Seq(DayPoint("2002-01-05", 231, 1), DayPoint("2002-01-06", 176, 0)))
  }

  private val waitingLog =
    """[main] INFO model.CoupledBiophysicalModel - Simulation run started
      |[main] INFO model.CoupledBiophysicalModel - Day 2019-11-14 has been completed in 2 secs with 1209 larvae.
      |[main] INFO model.CoupledBiophysicalModel - Day 2019-11-15 has been completed in 91 secs with 4048 larvae (waited 89 secs for flow data).
      |[main] INFO io.FlowFileIterator - Waiting for the flow data for 2019-11-17 from https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily: 10 secs so far
      |[main] INFO io.FlowFileIterator - Waiting for the flow data for 2019-11-17 from https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily: 20 secs so far
      |""".stripMargin

  it should "say how much of each day was spent waiting for flow data" in {
    val days = RunProgress.parse(waitingLog).days
    assert(days == Seq(DayPoint("2019-11-14", 1209, 2, 0), DayPoint("2019-11-15", 4048, 91, 89)))
  }

  it should "say how many larvae are from each release site, when the log does" in {
    val line = "[main] INFO model.CoupledBiophysicalModel - Day 2019-11-15 has been completed in 91 secs with 4048 larvae (waited 89 secs for flow data) [by site: Eden (south)=1000|Sydney=3048]."
    val day = RunProgress.parse(line).days.head
    assert(day == DayPoint("2019-11-15", 4048, 91, 89, Seq(SiteCount("Eden (south)", 1000), SiteCount("Sydney", 3048))))
  }

  it should "say when the run is waiting for flow data now, and for how long" in {
    val summary = RunProgress.parse(waitingLog)
    assert(summary.waiting.contains(FlowWait("2019-11-17", 20, "https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily")))
  }

  it should "stop saying it is waiting once a day has been completed since, or the run has finished" in {
    val moved = waitingLog + "[main] INFO model.CoupledBiophysicalModel - Day 2019-11-16 has been completed in 12 secs with 3000 larvae (waited 11 secs for flow data).\n"
    assert(RunProgress.parse(moved).waiting.isEmpty)
    val done = waitingLog + "[main] INFO model.CoupledBiophysicalModel - Simulation run completed in 3.00 minutes\n"
    assert(RunProgress.parse(done).waiting.isEmpty)
    assert(RunProgress.parse(log).waiting.isEmpty, "a run that never waited")
  }

  it should "notice that the run completed" in {
    assert(RunProgress.parse(log).finished)
    assert(!RunProgress.parse(log.linesIterator.take(4).mkString("\n")).finished)
  }

  it should "pass warnings on without the logger's prefix" in {
    assert(RunProgress.parse(log).warnings == Seq("654 larvae left the edge of the flow grid and were killed"))
  }

  it should "find exceptions, which the simulator reports but still exits normally" in {
    val failing = "java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 81\n\tat x.y(Z.scala:1)"
    assert(RunProgress.parse(failing).problems == Seq("java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 81"))
  }

  it should "cope with an empty log" in {
    val summary = RunProgress.parse("")
    assert(summary.days.isEmpty && !summary.finished && summary.warnings.isEmpty && summary.problems.isEmpty)
  }

  "The fraction through the period" should "follow the last completed day" in {
    val summary = RunProgress.parse(log)
    val fraction = RunProgress.fraction(summary, "2002-01-05", "2002-01-15").get
    assert(math.abs(fraction - 0.1) < 1e-9)
  }

  it should "stay between 0 and 1, and be unknown before any day completes" in {
    val summary = RunProgress.parse(log)
    assert(RunProgress.fraction(summary, "2001-01-01", "2001-01-02").contains(1.0))
    assert(RunProgress.fraction(RunProgress.parse(""), "2002-01-05", "2002-01-15").isEmpty)
    assert(RunProgress.fraction(summary, "2002-01-05", "2002-01-05").isEmpty)
  }
}

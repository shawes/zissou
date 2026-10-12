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

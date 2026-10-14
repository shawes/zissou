package ui

import java.nio.file.{Files, Path}
import io.config.ConfigurationFile
import org.scalatest.flatspec.AnyFlatSpec

class RunManagerTest extends AnyFlatSpec {

  // Stand-ins for the simulator. $0 is the path of the run's config.yaml.
  private val succeeds =
    """d="$(dirname "$0")/output/config"; mkdir -p "$d"
      |echo '[main] INFO m - Day 2002-01-05 has been completed in 0 secs with 10 larvae.'
      |printf 'id,born,region,source,age,recruited-date,settle-id\na,d,patch 101,101,5.0,d,101\n' > "$d/x-connectivity-matrix.csv"
      |""".stripMargin
  private val throwsButExitsNormally = "echo 'java.lang.IllegalStateException: boom'; exit 0"
  private val exitsWithCode = "echo something went wrong; exit 3"
  private val runsForAWhile = "echo started; sleep 30"

  private def manager(script: String): (RunManager, Path) = {
    val base = Files.createTempDirectory("zissou-runs-test")
    (new RunManager(base.resolve("runs"), base, config => Seq("sh", "-c", script, config.toString)), base)
  }

  private def waitFor(runs: RunManager, id: String)(done: RunInfo => Boolean): RunInfo = {
    val deadline = System.currentTimeMillis() + 15000
    while (System.currentTimeMillis() < deadline && !runs.get(id).exists(done)) Thread.sleep(25)
    runs.get(id).getOrElse(fail(s"no run $id"))
  }

  private def finished(runs: RunManager, id: String) = waitFor(runs, id)(_.status != "running")

  "A run" should "succeed when the simulator exits normally and writes results" in {
    val (runs, _) = manager(succeeds)
    val started = runs.start("smoke.yaml", SampleConfig.yaml()).toOption.get
    assert(started.status == "running" && started.config == "smoke.yaml")
    val done = finished(runs, started.id)
    assert(done.status == "succeeded" && done.exitCode.contains(0) && done.endedAt.isDefined && done.message.isEmpty)
    assert(runs.results(started.id).get.settled == 1)
    assert(runs.detail(started.id).get.log.days.map(_.larvae) == Seq(10))
  }

  it should "fail, with the exception, when the simulator exits normally but wrote nothing" in {
    val (runs, _) = manager(throwsButExitsNormally)
    val done = finished(runs, runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id)
    assert(done.status == "failed" && done.message.contains("java.lang.IllegalStateException: boom"))
  }

  it should "fail with the exit code when the simulator exits with an error" in {
    val (runs, _) = manager(exitsWithCode)
    val done = finished(runs, runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id)
    assert(done.status == "failed" && done.exitCode.contains(3) && done.message.contains("The simulator exited with code 3."))
  }

  it should "be cancelled by killing the process, quickly" in {
    val (runs, _) = manager(runsForAWhile)
    val started = runs.start("x.yaml", SampleConfig.yaml()).toOption.get
    waitFor(runs, started.id)(_ => runs.log(started.id, 0).exists(_._1.contains("started")))
    assert(runs.cancel(started.id))
    val done = finished(runs, started.id)
    assert(done.status == "cancelled")
    assert(done.endedAt.get - done.startedAt < 10000, "it should not have waited for the sleep to finish")
    assert(!runs.cancel(started.id), "a finished run cannot be cancelled")
  }

  it should "not start while another run is going" in {
    val (runs, _) = manager(runsForAWhile)
    val first = runs.start("x.yaml", SampleConfig.yaml()).toOption.get
    assert(runs.start("y.yaml", SampleConfig.yaml()) == Left("A run is already in progress."))
    runs.cancel(first.id)
    finished(runs, first.id)
    assert(runs.start("y.yaml", SampleConfig.yaml()).isRight)
    runs.shutdown()
  }

  it should "run several at once when allowed, up to the limit" in {
    val base = Files.createTempDirectory("zissou-runs-test")
    val runs = new RunManager(base.resolve("runs"), base, config => Seq("sh", "-c", runsForAWhile, config.toString), None, 2)
    val first = runs.start("x.yaml", SampleConfig.yaml()).toOption.get
    val second = runs.start("y.yaml", SampleConfig.yaml()).toOption.get
    assert(runs.runningCount == 2)
    assert(runs.start("z.yaml", SampleConfig.yaml()) == Left("2 runs are already in progress."))
    runs.cancel(first.id)
    finished(runs, first.id)
    assert(runs.get(second.id).get.status == "running")
    assert(runs.start("z.yaml", SampleConfig.yaml()).isRight)
    runs.shutdown()
  }

  it should "not start from a configuration that does not parse" in {
    val (runs, _) = manager(succeeds)
    val result = runs.start("x.yaml", "settings: nonsense")
    assert(result.left.exists(_.startsWith("The configuration is not valid")))
    assert(runs.list().isEmpty)
  }

  it should "run on a copy of the configuration pointed at the run's own folder" in {
    val (runs, base) = manager(succeeds)
    val started = runs.start("x.yaml", SampleConfig.yaml(flow = "my/flow")).toOption.get
    finished(runs, started.id)
    val derived = new ConfigurationFile().parse(Files.readString(base.resolve("runs").resolve(started.id).resolve("config.yaml"))).toOption.get
    assert(derived.output.saveOutputFilePath == base.resolve("runs").resolve(started.id).resolve("output").toAbsolutePath.toString)
    assert(derived.output.logFile.isEmpty, "the log goes to the console, which the run captures")
    assert(derived.flow.netcdfFilePath == "my/flow" && derived.flow.period.start == "2002-01-05")
    assert(Files.readString(base.resolve("runs").resolve(started.id).resolve("source.yaml")) == SampleConfig.yaml(flow = "my/flow"))
  }

  it should "read the log from an offset" in {
    val (runs, _) = manager(succeeds)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, id)
    val (all, next) = runs.log(id, 0).get
    assert(all.contains("Day 2002-01-05") && next == all.getBytes("UTF-8").length)
    assert(runs.log(id, next).get == ("", next))
    assert(runs.log(id, 100000).get._1.isEmpty)
    assert(runs.log("20000101-000000-nope", 0).isEmpty)
  }

  "The run history" should "survive a restart, and mark a run cut off by it as interrupted" in {
    val (runs, base) = manager(succeeds)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, id)
    val staleId = "20260101-120000-stale"
    Files.createDirectories(base.resolve("runs").resolve(staleId))
    Files.writeString(base.resolve("runs").resolve(staleId).resolve("run.json"),
      """{"id":"20260101-120000-stale","config":"s.yaml","status":"running","startedAt":1,"endedAt":null,"exitCode":null,"message":null,"periodStart":"2002-01-05","periodEnd":"2002-02-20"}""")

    val reopened = new RunManager(base.resolve("runs"), base, _ => Seq("true"))
    assert(reopened.list().map(_.id).toSet == Set(id, staleId))
    assert(reopened.get(id).get.status == "succeeded")
    assert(reopened.get(staleId).get.status == "interrupted")
    assert(!reopened.isRunning)
  }

  it should "list the newest run first" in {
    val (runs, _) = manager(succeeds)
    val first = runs.start("a.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, first)
    Thread.sleep(5)
    val second = runs.start("b.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, second)
    assert(runs.list().map(_.id) == Seq(second, first))
  }

  "Archiving a run" should "put it away and bring it back, and keep that over a restart" in {
    val (runs, base) = manager(succeeds)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, id)
    assert(runs.get(id).get.archived.isEmpty)
    assert(runs.setArchived(id, true).toOption.get.archived.contains(true))
    val reopened = new RunManager(base.resolve("runs"), base, _ => Seq("true"))
    assert(reopened.get(id).get.archived.contains(true))
    assert(runs.setArchived(id, false).toOption.get.archived.isEmpty)
    assert(runs.setArchived("20200101-000000-none", true) == Left("No such run."))
  }

  it should "refuse while the run is going" in {
    val (runs, _) = manager(runsForAWhile)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    assert(runs.setArchived(id, true) == Left("A run that is going cannot be archived."))
    runs.cancel(id)
    finished(runs, id)
  }

  "Deleting a run" should "remove it and everything it wrote" in {
    val (runs, base) = manager(succeeds)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, id)
    assert(runs.delete(id).isRight)
    assert(runs.get(id).isEmpty && !Files.exists(base.resolve("runs").resolve(id)))
    assert(runs.delete(id) == Left("No such run."))
  }

  it should "refuse while the run is going" in {
    val (runs, base) = manager(runsForAWhile)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    assert(runs.delete(id) == Left("Cancel the run before deleting it."))
    assert(Files.exists(base.resolve("runs").resolve(id)))
    runs.cancel(id)
    finished(runs, id)
  }

  "A run id" should "only ever name a folder inside the runs folder" in {
    val (runs, _) = manager(succeeds)
    val id = runs.start("x.yaml", SampleConfig.yaml()).toOption.get.id
    finished(runs, id)
    assert(runs.dirOf(id).isDefined)
    assert(Seq("../etc", "..", "", "20260101-120000-nope", s"$id/..", s"../$id").forall(runs.dirOf(_).isEmpty))
  }
}

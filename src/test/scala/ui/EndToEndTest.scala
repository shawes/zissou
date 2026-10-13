package ui

import java.nio.file.{Files, Path}
import java.nio.{ByteBuffer, ByteOrder}
import org.scalatest.flatspec.AnyFlatSpec

/** Runs the real simulator, in a second JVM, on generated data. */
class EndToEndTest extends AnyFlatSpec {

  "The interface" should "run the real simulator and read what it wrote" in {
    val base = Files.createTempDirectory("zissou-e2e-test")
    tools.SyntheticDataset.main(Array(base.toString))
    val configs = new ConfigStore(base, base)
    val runs = new RunManager(base.resolve("runs"), base, RunManager.jvmCommand(1))

    val check = configs.validate(configs.read("smoke.yaml").get)
    assert(check.ok, check.error.toString)
    // the generated configs do trip the pldType warning, but nothing may be wrong with
    // their files, grid or period
    val problems = check.warnings.filter(w =>
      Seq("does not exist", "sub-folder", "end immediately", "outside the flow region").exists(w.contains))
    assert(problems.isEmpty, problems.toString)

    val started = runs.start("smoke.yaml", configs.read("smoke.yaml").get).toOption.get
    val deadline = System.currentTimeMillis() + 120000
    while (System.currentTimeMillis() < deadline && runs.get(started.id).exists(_.status == "running")) Thread.sleep(100)
    val done = runs.get(started.id).get
    assert(done.status == "succeeded", s"${done.status}: ${done.message}\n${runs.log(started.id, 0).map(_._1).getOrElse("")}")

    val results = runs.results(started.id).get
    assert(results.settled > 10, s"only ${results.settled} larvae settled")
    assert(results.kernel.exists(_.count == results.settled))
    assert(runs.detail(started.id).get.log.finished)

    // and the map is drawable from what was written: reefs, sites, and paths that start at a release site
    val map = runs.map(started.id, 400).get
    assert(map.notes.isEmpty, map.notes.toString)
    assert(map.reefs.map(_.patch).toSet == Set(101, 104, 107) && map.sites.size == 3, "only the reefs the three sites release from")
    assert(map.settled.total == results.settled && map.settled.tracks.nonEmpty)
    assert(map.settled.tracks.forall(t => t.points.size >= 2 && t.points.forall(p => p(0) >= map.bounds(0) && p(0) <= map.bounds(2) && p(1) >= map.bounds(1) && p(1) <= map.bounds(3))))
    assert(map.settled.tracks.forall { t =>
      val site = map.sites(t.site)
      math.abs(t.points.head(0) - site.longitude) < 0.02 && math.abs(t.points.head(1) - site.latitude) < 0.02
    }, "every path should start at the release site it was assigned to")

    // The settled-larvae shapefile must hold real lines. GeoTools writes an empty
    // geometry (type 0) when its plug-in files are missing, as they were in a fat jar.
    val shapefile = ResultsReader.outputDir(runs.dirOf(started.id).get).get.resolve("smoke-settled.shp")
    val firstRecordType = ByteBuffer.wrap(Files.readAllBytes(shapefile), 108, 4).order(ByteOrder.LITTLE_ENDIAN).getInt
    assert(firstRecordType == 3, s"the first record has shape type $firstRecordType, expected 3 (a line)")
  }

  it should "stop, with a warning, when the flow files run out, and keep the results of what it simulated" in {
    val base = Files.createTempDirectory("zissou-e2e-flow-ends-test")
    tools.SyntheticDataset.main(Array(base.toString))
    // January and February only, so the flow ends on 28 February
    for (variable <- Seq("u", "v", "w")) Files.delete(base.resolve("flow").resolve(variable).resolve(s"ocean_${variable}_2002_03.nc"))
    val configs = new ConfigStore(base, base)
    // larvae are still being released on 25 February, so some are in the water when the flow stops
    val text = configs.read("smoke.yaml").get
      .replace("end: 2002-01-20", "end: 2002-02-25")
      .replace("end: 2002-02-20", "end: 2002-03-10")
    assert(configs.validate(text).ok)

    val runs = new RunManager(base.resolve("runs"), base, RunManager.jvmCommand(1))
    val started = runs.start("smoke.yaml", text).toOption.get
    val deadline = System.currentTimeMillis() + 120000
    while (System.currentTimeMillis() < deadline && runs.get(started.id).exists(_.status == "running")) Thread.sleep(100)

    val done = runs.get(started.id).get
    assert(done.status == "succeeded", s"${done.status}: ${done.message}\n${runs.log(started.id, 0).map(_._1).getOrElse("")}")
    val warnings = runs.detail(started.id).get.log.warnings
    assert(warnings.exists(w => w.contains("hold nothing after 2002-02-28") && w.contains("The simulation stops at 2002-02-28T12:00")), warnings.toString)
    val results = runs.results(started.id).get
    assert(results.settled > 0, "what was simulated before the flow ran out is kept")
    assert(!runs.log(started.id, 0).get._1.contains("Exception"))
  }
}

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
    val runs = new RunManager(base.resolve("runs"), base, RunManager.jvmCommand)

    val check = configs.validate(configs.read("smoke.yaml").get)
    assert(check.ok, check.error.toString)
    // the generated configs do trip two known-issue warnings (interval 5, pldType Fixed),
    // but nothing may be wrong with their files, grid or period
    val problems = check.warnings.filter(w =>
      Seq("does not exist", "sub-folder", "end immediately", "outside the supported").exists(w.contains))
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

    // The settled-larvae shapefile must hold real lines. GeoTools writes an empty
    // geometry (type 0) when its plug-in files are missing, as they were in a fat jar.
    val shapefile = ResultsReader.outputDir(runs.dirOf(started.id).get).get.resolve("smoke-settled.shp")
    val firstRecordType = ByteBuffer.wrap(Files.readAllBytes(shapefile), 108, 4).order(ByteOrder.LITTLE_ENDIAN).getInt
    assert(firstRecordType == 3, s"the first record has shape type $firstRecordType, expected 3 (a line)")
  }
}

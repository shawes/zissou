package ui

import java.nio.file.{Files, Path}
import org.scalatest.flatspec.AnyFlatSpec

class ResultsReaderTest extends AnyFlatSpec {

  private def runWith(matrixRows: Seq[String], kernelRows: Seq[String]): Path = {
    val run = Files.createTempDirectory("zissou-results-test")
    val out = Files.createDirectories(run.resolve("output").resolve("config"))
    Files.writeString(out.resolve("x-connectivity-matrix.csv"),
      ("id,born,region,source,age,recruited-date,settle-id" +: matrixRows).mkString("\n") + "\n")
    Files.writeString(out.resolve("x-dispersal-kernel.csv"),
      ("id,born,birthplace,birth-reef,age,settle-reef,dispersal-distance,crow-flies-distance" +: kernelRows).mkString("\n") + "\n")
    Files.writeString(out.resolve("x-settled.shp"), "shape")
    run
  }

  "The results reader" should "have nothing to read before a run has written output" in {
    val run = Files.createTempDirectory("zissou-empty-run")
    assert(ResultsReader.outputDir(run).isEmpty && ResultsReader.read(run).isEmpty)
    Files.createDirectories(run.resolve("output").resolve("config"))
    assert(ResultsReader.read(run).isEmpty)
  }

  it should "count settlements from each source patch to each settlement patch" in {
    val run = runWith(
      Seq("a,2002-01-05,patch 101,101,5.0,2002-01-10,101", "b,2002-01-05,patch 101,101,5.0,2002-01-10,102",
        "c,2002-01-05,patch 101,101,5.0,2002-01-10,102", "d,2002-01-05,patch 104,104,5.0,2002-01-10,104"), Seq.empty)
    val results = ResultsReader.read(run).get
    assert(results.settled == 4)
    assert(results.matrix.sources == Seq(101, 104) && results.matrix.sinks == Seq(101, 102, 104))
    assert(results.matrix.counts == Seq(Seq(1, 2, 0), Seq(0, 0, 1)))
    assert(results.sites == Seq(SiteTotal(101, "patch 101", 3, 1), SiteTotal(104, "patch 104", 1, 1)))
  }

  it should "describe how far larvae travelled, in kilometres" in {
    val run = runWith(Seq("a,d,patch 101,101,5.0,d,101"), Seq(
      "a,d,patch 101,101,4.0,101,20000.0,10000.0",
      "b,d,patch 101,101,6.0,102,40000.0,30000.0",
      "c,d,patch 101,101,5.0,102,60000.0,50000.0"))
    val kernel = ResultsReader.read(run).get.kernel.get
    assert(kernel.count == 3)
    assert(kernel.meanCrowFliesKm == 30.0 && kernel.medianCrowFliesKm == 30.0 && kernel.maxCrowFliesKm == 50.0)
    assert(kernel.meanDispersalKm == 40.0 && kernel.meanAgeDays == 5.0)
    assert(kernel.histogram.size == 10 && kernel.histogram.map(_.count).sum == 3)
    assert(kernel.histogram.last.count == 1, "the furthest larva belongs in the last bin")
  }

  it should "give no kernel when no larva settled" in {
    assert(ResultsReader.read(runWith(Seq.empty, Seq.empty)).get.kernel.isEmpty)
  }

  it should "skip malformed rows instead of failing" in {
    val run = runWith(Seq("a,d,patch 101,101,5.0,d,101", "garbage", "b,d,patch 101,xx,5.0,d,101"), Seq("short"))
    val results = ResultsReader.read(run).get
    assert(results.settled == 1 && results.kernel.isEmpty)
  }

  it should "list the files with their sizes" in {
    val files = ResultsReader.read(runWith(Seq("a,d,p,1,5.0,d,1"), Seq.empty)).get.files
    assert(files.map(_.name) == Seq("x-connectivity-matrix.csv", "x-dispersal-kernel.csv", "x-settled.shp"))
    assert(files.last.bytes == 5)
  }
}

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
    // every reef that was a source or a sink, once, in the one order, and [row, column, larvae] where there were any
    assert(results.matrix.patches.map(p => (p.patch, p.name)) == Seq((101, "patch 101"), (102, ""), (104, "patch 104")))
    assert(results.matrix.cells == Seq(Seq(0, 0, 1), Seq(0, 1, 2), Seq(2, 2, 1)))
    assert(results.matrix.peak == 2)
    assert(results.sites == Seq(SiteTotal(101, "patch 101", 3, 1), SiteTotal(104, "patch 104", 1, 1)))
  }

  it should "put the reefs in order from north to south, the same on both axes, when it knows where they are" in {
    val run = runWith(
      Seq("a,d,patch 101,101,5.0,d,104", "b,d,patch 104,104,5.0,d,102", "c,d,patch 104,104,5.0,d,104"), Seq.empty)
    // 102 is the most northerly, then 101, then 104
    val base = Files.createTempDirectory("zissou-results-habitat")
    tools.ShapefileTestUtil.writeReefs(base.resolve("reefs.shp").toFile, Seq(
      (101, "Reef", Seq((153.65, -28.85), (153.65, -28.75), (153.75, -28.75), (153.75, -28.85))),
      (102, "Reef", Seq((153.85, -28.55), (153.85, -28.45), (153.95, -28.45), (153.95, -28.55))),
      (104, "Reef", Seq((154.0, -29.05), (154.0, -28.95), (154.1, -28.95), (154.1, -29.05)))))
    Files.writeString(run.resolve("config.yaml"), SampleConfig.yaml(habitat = base.resolve("reefs.shp").toString))
    val matrix = ResultsReader.read(run, Some(base)).get.matrix
    assert(matrix.patches.map(_.patch) == Seq(102, 101, 104))
    assert(matrix.patches.forall(p => p.latitude.isDefined && p.longitude.isDefined))
    assert(math.abs(matrix.patches.head.latitude.get - -28.5) < 1e-6)
    // 101 -> 104 is row 1, column 2; 104 -> 102 is row 2, column 0; 104 -> 104 is the diagonal
    assert(matrix.cells == Seq(Seq(1, 2, 1), Seq(2, 0, 1), Seq(2, 2, 1)))
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
    assert(kernel.bands.map(_.count).sum == 3, "every larva is in a band")
    // 10, 30 and 50 km: the last band is 20 to 50 km, which holds the 30 and the furthest, 50
    assert(kernel.bands.last.count == 2 && kernel.bands.last.toKm.contains(50.0), "the furthest larva belongs in the last band")
  }

  it should "group distances into bands that grow, so that larvae settling close to home are still told apart" in {
    // (path, straight line) of each larva, in km
    val bands = ResultsReader.bands(Seq((0.0, 0.0), (0.0, 0.0), (1.0, 0.5), (2.0, 1.5), (4.0, 3.0), (20.0, 12.0), (300.0, 205.0)))
    assert(bands.map(b => (b.fromKm, b.toKm, b.count)) == Seq(
      (0.0, Some(0.0), 2), (0.0, Some(1.0), 1), (1.0, Some(2.0), 1), (2.0, Some(5.0), 1), (5.0, Some(10.0), 0),
      (10.0, Some(20.0), 1), (20.0, Some(50.0), 0), (50.0, Some(100.0), 0), (100.0, Some(200.0), 0), (200.0, Some(500.0), 1)))
    assert(bands.map(_.count).sum == 7)
    // a larva that goes further than any edge opens the last band
    val far = ResultsReader.bands(Seq((1.0, 1.0), (9000.0, 9000.0)))
    assert(far.last.toKm.isEmpty && far.last.fromKm == 5000.0 && far.last.count == 1)
    assert(ResultsReader.bands(Seq.empty).map(_.count).sum == 0)
  }

  it should "count a larva that never moved as staying even if its depth changed, and one that came back as moving" in {
    // never moved but its depth changed: a path of nothing and a straight line of 30 m; moved and ended where it began
    val bands = ResultsReader.bands(Seq((0.0, 0.03), (0.0, 0.0), (6.0, 0.0), (6.0, 2.0)))
    assert(bands.head.count == 2, "the two that never moved")
    assert(bands(1).count == 1 && bands(1).toKm.contains(1.0), "the one that came back, in the first band of those that moved")
    assert(bands.size == 3 && bands(2).count == 1 && bands(2).toKm.contains(2.0), "and the one at 2 km in the next")
  }

  it should "say within what distance a share of the larvae settled" in {
    val km = (1 to 100).map(_.toDouble)
    assert(Seq(50, 90, 99, 100).map(ResultsReader.percentile(km, _)) == Seq(50.0, 90.0, 99.0, 100.0))
    assert(ResultsReader.percentile(Seq.empty, 50) == 0.0 && ResultsReader.percentile(Seq(7.0), 99) == 7.0)
  }

  it should "count larvae that never moved, and give the distances of each release reef" in {
    val run = runWith(Seq("a,d,patch 101,101,5.0,d,101", "b,d,patch 101,101,5.0,d,101", "c,d,patch 102,102,5.0,d,102", "e,d,patch 102,102,5.0,d,101"), Seq(
      "a,d,patch 101,101,5.0,101,0.0,0.0",
      "b,d,patch 101,101,5.0,101,8000.0,4000.0",
      "c,d,patch 102,102,5.0,102,0.0,0.0",
      "e,d,patch 102,102,5.0,101,60000.0,30000.0"))
    val kernel = ResultsReader.read(run).get.kernel.get
    assert(kernel.stayed == 2)
    assert(kernel.bySite == Seq(
      // of two larvae, the median is the nearer
      SiteDistance(101, "patch 101", 2, 1, 0.0, 4.0, 4.0), SiteDistance(102, "patch 102", 2, 1, 0.0, 30.0, 30.0)))
    assert(kernel.percentiles.map(_.percent) == Seq(50, 75, 90, 95, 99))
    assert(kernel.percentiles.last.km == 30.0)
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

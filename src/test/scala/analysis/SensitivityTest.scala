package analysis

import java.nio.file.{Files, Path}
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import scala.jdk.CollectionConverters.*
import ui.{Matrix, MatrixPatch, Results, SiteTotal}

class SensitivityTest extends AnyFlatSpec {

  private def results(links: Seq[(Int, Int, Int)], lat: Map[Int, Double]): Results = {
    val patches = (links.map(_._1) ++ links.map(_._2)).distinct.sorted.map(p => MatrixPatch(p, s"site $p", lat.get(p), Some(153.0)))
    val index = patches.map(_.patch).zipWithIndex.toMap
    val sites = links.map(_._1).distinct.sorted.map(s => SiteTotal(s, s"site $s", links.filter(_._1 == s).map(_._3).sum,
      links.filter(l => l._1 == s && l._2 == s).map(_._3).sum))
    Results(links.map(_._3).sum, Matrix(patches, links.map(l => Seq(index(l._1), index(l._2), l._3)), links.map(_._3).max), sites, None, Nil)
  }

  "The outputs of a run" should "count self-recruitment, settlers north of their source, and settlers in each region" in {
    // reef 1 is the furthest north; 2,000,001 is in the region
    val r = results(Seq((1, 1, 30), (2, 1, 10), (2, 2, 40), (2, 2000001, 20)), Map(1 -> -30.0, 2 -> -31.0, 2000001 -> -38.0))
    val m = RunMetrics.of(r, "x\n[main] INFO model.CoupledBiophysicalModel - 400 larvae were released\n", Seq(1, 5, 12, 30), Seq(("south", 2000000L, 2999999L)))
    assert(m.settled == 100 && m.released.contains(400) && m.settlement.contains(0.25))
    assert(m.selfRecruitment == 0.7)
    assert(m.northward == 0.1, "the 10 from reef 2 to reef 1")
    assert(m.localRetention == 0.5, "two of the four distances are within 10 km")
    assert(m.regions == Seq("south" -> 0.2))
  }

  "Connectivity compared with the baseline's" should "be the same, or have nothing in common, or between" in {
    val base = RunMetrics.of(results(Seq((1, 1, 10), (1, 2, 10)), Map.empty), "", Nil, Nil)
    assert(base.brayCurtis(base) == 0.0 && base.linksKept(base) == 1.0)
    val apart = RunMetrics.of(results(Seq((2, 2, 5)), Map.empty), "", Nil, Nil)
    assert(apart.brayCurtis(base) == 1.0 && apart.linksKept(base) == 0.0)
    val half = RunMetrics.of(results(Seq((1, 1, 10)), Map.empty), "", Nil, Nil)
    assert(half.brayCurtis(base) == 10.0 / 30 && half.linksKept(base) == 0.5)
  }

  "A design" should "run each configuration, not run again what it has, and sum them up" in {
    val base = Files.createTempDirectory("zissou-sensitivity-test")
    tools.SyntheticDataset.main(Array(base.toString))
    val design = Design(base.resolve("smoke.yaml"), base.resolve("sensitivity"), 2, None, Nil, Nil, Seq(
      PlannedRun("baseline", Nil, None, "baseline"),
      PlannedRun("fewer", Seq("spawn.spawningLocation[*].numberOfLarvae" -> Json.fromInt(20)), None, "fewer")
    ))
    val configs = Sensitivity.configurations(design).fold(fail(_), identity)
    assert(configs.size == 2 && configs(1)._2.contains("numberOfLarvae: 20"))
    assert(Sensitivity.run(design, configs, base).isEmpty)
    val written = Files.getLastModifiedTime(base.resolve("sensitivity").resolve("fewer").resolve("run.log"))
    assert(Sensitivity.run(design, configs, base).isEmpty)
    assert(Files.getLastModifiedTime(base.resolve("sensitivity").resolve("fewer").resolve("run.log")) == written, "not run again")

    val lines = Files.readAllLines(Sensitivity.writeSummary(design, base)).asScala
    val header = lines.head.split(",").toSeq
    val rows = lines.tail.map(l => header.zip(l.split(",", -1)).toMap)
    assert(rows.map(_("run")) == Seq("baseline", "fewer") && rows.forall(_("status") == "done"))
    assert(rows(1)("spawn.spawningLocation[*].numberOfLarvae") == "20")
    val released = rows.map(_("released").toLong)
    assert(released(1) * 5 == released(0), s"a fifth as many larvae: $released")
    assert(rows.head("bray_curtis_vs_baseline") == "0.0000" && rows.head("baseline_links_kept_pct") == "100.00")
    assert(rows(1)("bray_curtis_vs_baseline").toDouble > 0)
  }

  it should "not start when a run's settings do not make a valid configuration" in {
    val base = Files.createTempDirectory("zissou-sensitivity-invalid")
    tools.SyntheticDataset.main(Array(base.toString))
    val design = Design(base.resolve("smoke.yaml"), base.resolve("s"), 1, None, Nil, Nil,
      Seq(PlannedRun("bad", Seq("flow.timeStep.duration" -> Json.fromString("soon")), None, "bad")))
    assert(Sensitivity.configurations(design).left.toOption.get.startsWith("The run bad does not give a valid configuration"))
  }
}

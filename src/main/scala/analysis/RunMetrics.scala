package analysis

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Try
import ui.{Results, ResultsReader}

/** What a run came to, as the outputs a sensitivity analysis compares. */
case class RunMetrics(
    released: Option[Long],
    settled: Int,
    medianKm: Double,
    p90Km: Double,
    maxKm: Double,
    meanAgeDays: Double,
    selfRecruitment: Double, // share of the settlers that settled on the reef they were released from
    localRetention: Double, // share of the settlers that settled within LocalKm of where they were released
    northward: Double, // share of the settlers that settled north of where they were released
    regions: Seq[(String, Double)], // share of the settlers on the reefs of each region
    minutes: Option[Double],
    bySite: Seq[(String, Int, Double)], // each release site: its settlers and their median distance
    links: Map[(Int, Int), Int] // larvae from each reef to each reef
) {
  def settlement: Option[Double] = released.filter(_ > 0).map(settled.toDouble / _)

  /** How different the connectivity is from another run's: 0 the same, 1 nothing in common (Bray–Curtis). */
  def brayCurtis(other: RunMetrics): Double = {
    val pairs = links.keySet ++ other.links.keySet
    val total = pairs.toSeq.map(p => links.getOrElse(p, 0) + other.links.getOrElse(p, 0)).sum
    if (total == 0) 0.0 else pairs.toSeq.map(p => math.abs(links.getOrElse(p, 0) - other.links.getOrElse(p, 0))).sum.toDouble / total
  }

  /** The share of another run's links, reef to reef, that this run has too. */
  def linksKept(other: RunMetrics): Double =
    if (other.links.isEmpty) 1.0 else other.links.keySet.count(links.contains).toDouble / other.links.size
}

object RunMetrics {

  /** The distance, in km, within which a settler counts as retained near where it was released. */
  val LocalKm = 10.0
  // a sink less than this far north of its source, in degrees (about a kilometre), is not counted as north of it
  private val NorthTolerance = 0.01

  private val Released = """(\d+) larvae were released""".r.unanchored
  private val Completed = """Simulation run completed in ([\d.]+) minutes""".r.unanchored

  /** Reads the outputs of a run folder laid out as the sensitivity runner lays it out, if it has results. */
  def read(runDir: Path, workDir: Path, regions: Seq[(String, Long, Long)]): Option[RunMetrics] =
    ResultsReader.read(runDir, Some(workDir)).map { results =>
      val log = Try(Files.readString(runDir.resolve("run.log"))).getOrElse("")
      val straightKm = ResultsReader.outputDir(runDir).toSeq.flatMap(kernelDistances)
      of(results, log, straightKm, regions)
    }

  def of(results: Results, log: String, straightKm: Seq[Double], regions: Seq[(String, Long, Long)]): RunMetrics = {
    val patches = results.matrix.patches
    val links = results.matrix.cells.map(c => (patches(c(0)).patch, patches(c(1)).patch) -> c(2)).toMap
    val settled = results.settled
    def share(n: Double) = if (settled == 0) 0.0 else n / settled
    val north = results.matrix.cells.map { c =>
      val (from, to) = (patches(c(0)).latitude, patches(c(1)).latitude)
      if (from.isDefined && to.isDefined && to.get > from.get + NorthTolerance) c(2) else 0
    }.sum
    val kernel = results.kernel
    RunMetrics(
      released = Released.findAllMatchIn(log).toSeq.lastOption.map(_.group(1).toLong),
      settled = settled,
      medianKm = kernel.map(_.medianCrowFliesKm).getOrElse(0.0),
      p90Km = kernel.flatMap(_.percentiles.find(_.percent == 90)).map(_.km).getOrElse(0.0),
      maxKm = kernel.map(_.maxCrowFliesKm).getOrElse(0.0),
      meanAgeDays = kernel.map(_.meanAgeDays).getOrElse(0.0),
      selfRecruitment = share(results.sites.map(_.returnedHome).sum),
      localRetention = if (straightKm.isEmpty) 0.0 else straightKm.count(_ <= LocalKm).toDouble / straightKm.size,
      northward = share(north),
      regions = regions.map { case (name, first, last) =>
        name -> share(links.collect { case ((_, to), n) if to >= first && to <= last => n }.sum)
      },
      minutes = Completed.findAllMatchIn(log).toSeq.lastOption.map(_.group(1).toDouble),
      bySite = results.sites.map { site =>
        (site.name, site.settled, kernel.flatMap(_.bySite.find(_.patch == site.patch)).map(_.medianKm).getOrElse(0.0))
      },
      links = links
    )
  }

  // the straight-line distance of each settler, in km: column 7 of the dispersal kernel, in metres
  private def kernelDistances(outputDir: Path): Seq[Double] = {
    val files = Files.list(outputDir)
    val kernel = try files.iterator().asScala.find(_.getFileName.toString.endsWith("-dispersal-kernel.csv")) finally files.close()
    kernel.toSeq.flatMap(f => Files.readAllLines(f).asScala.drop(1).flatMap(line => Try(line.split(",", -1)(7).toDouble / 1000).toOption))
  }
}

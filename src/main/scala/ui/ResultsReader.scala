package ui

import java.nio.file.{Files, Path}
import io.circe.Encoder
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** A reef that larvae were released from or settled on; `name` is the release site's, or empty. */
case class MatrixPatch(patch: Int, name: String, latitude: Option[Double], longitude: Option[Double]) derives Encoder.AsObject

/**
  * Source to sink: every reef involved, in the one order on both axes (north to south), and the larvae
  * that went between them as [row, column, count], for the pairs that had any. Rows are released from and
  * columns settled on, so the diagonal is larvae that settled where they were released.
  */
case class Matrix(patches: Seq[MatrixPatch], cells: Seq[Seq[Int]], peak: Int) derives Encoder.AsObject
/** Larvae that settled between `fromKm` (not included) and `toKm` (included) from where they were released; the first band, from nothing to nothing, is those that settled where they began. */
case class DistanceBand(fromKm: Double, toKm: Option[Double], count: Int) derives Encoder.AsObject
/** The distance within which `percent` of the larvae settled. */
case class Percentile(percent: Int, km: Double) derives Encoder.AsObject
case class SiteDistance(patch: Int, name: String, count: Int, stayed: Int, medianKm: Double, p90Km: Double, maxKm: Double) derives Encoder.AsObject
case class Kernel(
    count: Int,
    meanDispersalKm: Double,
    meanCrowFliesKm: Double,
    medianCrowFliesKm: Double,
    maxCrowFliesKm: Double,
    meanAgeDays: Double,
    stayed: Int, // larvae whose path was nothing: they settled where they were released without ever moving
    bands: Seq[DistanceBand],
    percentiles: Seq[Percentile],
    bySite: Seq[SiteDistance]
) derives Encoder.AsObject
case class SiteTotal(patch: Int, name: String, settled: Int, returnedHome: Int) derives Encoder.AsObject
case class OutputFile(name: String, bytes: Long) derives Encoder.AsObject
case class Results(
    settled: Int,
    matrix: Matrix,
    sites: Seq[SiteTotal],
    kernel: Option[Kernel],
    files: Seq[OutputFile]
) derives Encoder.AsObject

/** Reads the connectivity matrix and dispersal kernel a run wrote. */
object ResultsReader {

  /** The folder the simulator wrote to, if the run got as far as writing results. */
  def outputDir(runDir: Path): Option[Path] = {
    val output = runDir.resolve("output")
    if (!Files.isDirectory(output)) None
    else {
      val stream = Files.list(output)
      try stream.iterator().asScala.find(d =>
        Files.isDirectory(d) && Files.list(d).iterator().asScala.exists(_.getFileName.toString.endsWith("-connectivity-matrix.csv"))
      ) finally stream.close()
    }
  }

  // where each reef is, from the habitat layer a run used, read once: a layer can hold tens of thousands of reefs
  private val positions = new java.util.concurrent.ConcurrentHashMap[Path, Map[Int, (Double, Double)]]()

  private def positionsOf(runDir: Path, workDir: Path): Map[Int, (Double, Double)] =
    Try {
      val config = new io.config.ConfigurationFile().parse(Files.readString(runDir.resolve("config.yaml"))).toOption.get
      val file = workDir.resolve(config.habitat.shapeFilePath).toAbsolutePath
      positions.computeIfAbsent(file, f =>
        new io.GisShapeFile().read(f.toString).flatMap { feature =>
          for {
            geometry <- Option(feature.getDefaultGeometry).collect { case g: org.locationtech.jts.geom.Geometry => g }.filterNot(_.isEmpty)
            patch <- Option(feature.getAttribute("PATCH_NUM")).flatMap(a => Try(a.toString.toInt).toOption)
            centre = geometry.getCentroid
          } yield patch -> (centre.getY, centre.getX)
        }.toMap)
    }.getOrElse(Map.empty)

  /** `workDir` is where the run's habitat file is looked for, to put the reefs in order from north to south. */
  def read(runDir: Path, workDir: Option[Path] = None): Option[Results] =
    outputDir(runDir).map { dir =>
      val children = {
        val s = Files.list(dir)
        try s.iterator().asScala.toSeq.sortBy(_.getFileName.toString) finally s.close()
      }
      def csv(suffix: String): Seq[Array[String]] =
        children.find(_.getFileName.toString.endsWith(suffix)).toSeq.flatMap { p =>
          Files.readAllLines(p).asScala.drop(1).filter(_.nonEmpty).map(_.split(",", -1)).toSeq
        }

      // columns: id, born, region, source, age, recruited-date, settle-id
      val settledRows = csv("-connectivity-matrix.csv").flatMap { c =>
        for {
          source <- Try(c(3).toInt).toOption
          sink <- Try(c(6).toInt).toOption
        } yield (source, c(2), sink)
      }
      val sources = settledRows.map(_._1).distinct.sorted
      val names = settledRows.map(r => r._1 -> r._2).toMap
      val where = workDir.map(positionsOf(runDir, _)).getOrElse(Map.empty)
      // north to south where the reefs' positions are known, then by number
      val patches = (sources ++ settledRows.map(_._3)).distinct
        .sortBy(p => (where.get(p).map(-_._1).getOrElse(Double.PositiveInfinity), p))
        .map(p => MatrixPatch(p, names.getOrElse(p, ""), where.get(p).map(_._1), where.get(p).map(_._2)))
      val index = patches.map(_.patch).zipWithIndex.toMap
      val cells = settledRows.groupBy(r => (index(r._1), index(r._3))).toSeq
        .map { case ((row, column), larvae) => Seq(row, column, larvae.size) }
        .sortBy(c => (c(0), c(1)))
      val sites = sources.map { from =>
        val mine = settledRows.filter(_._1 == from)
        SiteTotal(from, mine.head._2, mine.size, mine.count(_._3 == from))
      }

      Results(
        settledRows.size,
        Matrix(patches, cells, cells.map(_(2)).maxOption.getOrElse(0)),
        sites,
        kernel(csv("-dispersal-kernel.csv")),
        children.map(p => OutputFile(p.getFileName.toString, Files.size(p)))
      )
    }

  // the edges of the bands of distance, in km: they grow, so that a run where most larvae settle close to home
  // still shows how the rest are spread, which equal bands up to the furthest larva would not
  private val BandEdges = Seq(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0)

  /**
    * The bands that reach the furthest larva: those that never moved (their path was nothing, whatever the
    * straight line to where they ended, which a change of depth alone makes more than nothing), then up to
    * 1 km, up to 2 ... and the last open-ended if need be. A larva that moved and ended where it began is in the first.
    * Each larva is its path and its straight-line distance, in km.
    */
  def bands(larvae: Seq[(Double, Double)]): Seq[DistanceBand] = {
    val stayed = larvae.count(_._1 <= 0.0)
    val moved = larvae.filter(_._1 > 0.0).map(_._2)
    val furthest = moved.maxOption.getOrElse(0.0)
    val edges = BandEdges.takeWhile(_ < furthest) ++ BandEdges.find(_ >= furthest)
    val lowers = 0.0 +: edges
    // the last band includes the furthest larva, and is open-ended if that is beyond the last edge
    val upper = edges.map(Option(_)) ++ (if (BandEdges.forall(_ < furthest)) Seq(None) else Seq.empty)
    DistanceBand(0.0, Some(0.0), stayed) +: lowers.zip(upper).zipWithIndex.map { case ((from, to), i) =>
      // the first band takes a larva that moved and ended where it began
      DistanceBand(from, to, moved.count(d => (d > from || (i == 0 && d >= from)) && to.forall(d <= _)))
    }
  }

  /** The distance within which a share of the larvae settled, by nearest rank. */
  def percentile(sortedKm: Seq[Double], percent: Int): Double =
    if (sortedKm.isEmpty) 0.0 else sortedKm(math.max(0, math.min(sortedKm.size - 1, math.ceil(percent / 100.0 * sortedKm.size).toInt - 1)))

  // columns: id, born, birthplace, birth-reef, age, settle-reef, dispersal-distance, crow-flies-distance (metres)
  private def kernel(rows: Seq[Array[String]]): Option[Kernel] = {
    val parsed = rows.flatMap { c =>
      for {
        age <- Try(c(4).toDouble).toOption
        path <- Try(c(6).toDouble / 1000).toOption
        straight <- Try(c(7).toDouble / 1000).toOption
        reef <- Try(c(3).toInt).toOption
      } yield (age, path, straight, reef, c(2))
    }
    if (parsed.isEmpty) None
    else {
      val straight = parsed.map(_._3).sorted
      val bySite = parsed.groupBy(_._4).toSeq.sortBy(_._1).map { case (reef, larvae) =>
        val distances = larvae.map(_._3).sorted
        SiteDistance(reef, larvae.head._5, larvae.size, larvae.count(_._2 <= 0.0), percentile(distances, 50), percentile(distances, 90), distances.last)
      }
      Some(
        Kernel(
          parsed.size,
          parsed.map(_._2).sum / parsed.size,
          straight.sum / straight.size,
          percentile(straight, 50),
          straight.last,
          parsed.map(_._1).sum / parsed.size,
          parsed.count(_._2 <= 0.0),
          bands(parsed.map(p => (p._2, p._3))),
          Seq(50, 75, 90, 95, 99).map(p => Percentile(p, percentile(straight, p))),
          bySite
        )
      )
    }
  }
}

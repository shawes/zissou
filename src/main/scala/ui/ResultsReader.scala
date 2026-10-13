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
case class HistogramBin(fromKm: Double, toKm: Double, count: Int) derives Encoder.AsObject
case class Kernel(
    count: Int,
    meanDispersalKm: Double,
    meanCrowFliesKm: Double,
    medianCrowFliesKm: Double,
    maxCrowFliesKm: Double,
    meanAgeDays: Double,
    histogram: Seq[HistogramBin]
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

  // columns: id, born, birthplace, birth-reef, age, settle-reef, dispersal-distance, crow-flies-distance (metres)
  private def kernel(rows: Seq[Array[String]]): Option[Kernel] = {
    val parsed = rows.flatMap { c =>
      for {
        age <- Try(c(4).toDouble).toOption
        path <- Try(c(6).toDouble / 1000).toOption
        straight <- Try(c(7).toDouble / 1000).toOption
      } yield (age, path, straight)
    }
    if (parsed.isEmpty) None
    else {
      val straight = parsed.map(_._3).sorted
      val max = straight.last
      val bins = 10
      val width = if (max <= 0) 1.0 else max / bins
      val histogram = (0 until bins).map { i =>
        val from = i * width
        val to = (i + 1) * width
        HistogramBin(from, to, straight.count(d => d >= from && (d < to || (i == bins - 1 && d <= to))))
      }
      Some(
        Kernel(
          parsed.size,
          parsed.map(_._2).sum / parsed.size,
          straight.sum / straight.size,
          straight(straight.size / 2),
          max,
          parsed.map(_._1).sum / parsed.size,
          histogram
        )
      )
    }
  }
}

package ui

import java.nio.file.{Files, Path}
import io.circe.Encoder
import io.config.ConfigurationFile
import io.GisShapeFile
import org.locationtech.jts.geom.Geometry
import scala.util.Try

case class Site(name: String, patch: Int, latitude: Double, longitude: Double) derives Encoder.AsObject
case class Reef(patch: Int, habitat: String, rings: Seq[Seq[Seq[Double]]]) derives Encoder.AsObject
/** One larva's path as [longitude, latitude] pairs; `site` is the release site it started nearest to. */
case class Track(site: Int, points: Seq[Seq[Double]]) derives Encoder.AsObject
case class TrackSet(total: Int, tracks: Seq[Track]) derives Encoder.AsObject
case class RunMap(
    bounds: Seq[Double], // west, south, east, north
    sites: Seq[Site],
    reefs: Seq[Reef],
    settled: TrackSet,
    sampled: TrackSet,
    notes: Seq[String]
) derives Encoder.AsObject

/** What the interface needs to draw a run on a map, read from the run's shapefiles. */
object MapReader {

  private val MaxPointsPerTrack = 24

  def read(runDir: Path, workDir: Path, trackLimit: Int): Option[RunMap] =
    ResultsReader.outputDir(runDir).flatMap { outputDir =>
      new ConfigurationFile().parse(Files.readString(runDir.resolve("config.yaml"))).toOption.map { config =>
        val notes = Seq.newBuilder[String]
        val sites = config.spawn.spawningLocation.map(s => Site(s.name, s.patchNumber, s.site.latitude, s.site.longitude))

        val reefs = readShapes(workDir.resolve(config.habitat.shapeFilePath)).fold(
          problem => { notes += problem; Seq.empty[Reef] },
          features => features.flatMap(reefOf)
        )
        def tracks(suffix: String): TrackSet = {
          val file = Option(outputDir.toFile.listFiles()).toSeq.flatten.find(_.getName.endsWith(suffix))
          file.map(f => readShapes(f.toPath)) match {
            case Some(Right(features)) =>
              val lines = features.flatMap(lineOf)
              if (features.nonEmpty && lines.isEmpty) {
                notes += s"The shapefile ${file.get.getName} holds no geometry, so it cannot be drawn. " +
                  "Runs made with the old single-jar build wrote empty shapefiles."
              }
              TrackSet(lines.size, thin(lines, trackLimit).map(points => Track(nearestSite(points.head, sites), simplify(points, MaxPointsPerTrack))))
            case Some(Left(problem)) => notes += problem; TrackSet(0, Seq.empty)
            case None => TrackSet(0, Seq.empty)
          }
        }
        val settled = tracks("-settled.shp")
        val sampled = tracks("-dispersal.shp")
        RunMap(bounds(sites, reefs, settled.tracks ++ sampled.tracks), sites, reefs, settled, sampled, notes.result())
      }
    }

  private def readShapes(file: Path): Either[String, Seq[org.geotools.api.feature.simple.SimpleFeature]] =
    if (!Files.isRegularFile(file)) Left(s"The shapefile ${file.getFileName} could not be found.")
    else Try(new GisShapeFile().read(file.toString)).toEither.left.map(e => s"The shapefile ${file.getFileName} could not be read: ${e.getMessage}")

  private def geometryOf(feature: org.geotools.api.feature.simple.SimpleFeature): Option[Geometry] =
    Option(feature.getDefaultGeometry).collect { case g: Geometry => g }.filterNot(_.isEmpty)

  private def lineOf(feature: org.geotools.api.feature.simple.SimpleFeature): Option[Seq[Seq[Double]]] =
    geometryOf(feature).map(_.getCoordinates.toSeq.map(c => Seq(round(c.x), round(c.y)))).filter(_.size >= 2)

  private def reefOf(feature: org.geotools.api.feature.simple.SimpleFeature): Option[Reef] =
    geometryOf(feature).map { geometry =>
      val polygons = (0 until geometry.getNumGeometries).map(geometry.getGeometryN).collect { case p: org.locationtech.jts.geom.Polygon => p }
      val rings = polygons.flatMap { p =>
        (p.getExteriorRing +: (0 until p.getNumInteriorRing).map(p.getInteriorRingN)).map(_.getCoordinates.toSeq.map(c => Seq(round(c.x), round(c.y))))
      }
      Reef(
        Option(feature.getAttribute("PATCH_NUM")).flatMap(a => Try(a.toString.toInt).toOption).getOrElse(-1),
        Option(feature.getAttribute("HABITAT")).map(_.toString).getOrElse(""),
        rings
      )
    }.filter(_.rings.nonEmpty)

  private def round(value: Double): Double = math.round(value * 10000.0) / 10000.0

  /** At most `max` items, evenly spread across the list and keeping its order. */
  def thin[A](items: Seq[A], max: Int): Seq[A] =
    if (max <= 0) Seq.empty
    else if (items.size <= max) items
    else (0 until max).map(i => items((i.toLong * items.size / max).toInt))

  /** At most `max` points of a path, evenly spread, always keeping the first and the last. */
  def simplify(points: Seq[Seq[Double]], max: Int): Seq[Seq[Double]] =
    if (points.size <= max || max < 2) points
    else (0 until max).map(i => points((i.toLong * (points.size - 1) / (max - 1)).toInt))

  /** The index of the release site nearest a [longitude, latitude] point, or -1 with no sites. */
  def nearestSite(point: Seq[Double], sites: Seq[Site]): Int =
    if (sites.isEmpty) -1
    else sites.indices.minBy { i =>
      val dx = (sites(i).longitude - point(0)) * math.cos(math.toRadians(sites(i).latitude))
      val dy = sites(i).latitude - point(1)
      dx * dx + dy * dy
    }

  /** West, south, east, north around everything drawn, with a margin; never empty. */
  def bounds(sites: Seq[Site], reefs: Seq[Reef], tracks: Seq[Track]): Seq[Double] = {
    val points = sites.map(s => (s.longitude, s.latitude)) ++
      reefs.flatMap(_.rings.flatten).map(p => (p(0), p(1))) ++ tracks.flatMap(_.points).map(p => (p(0), p(1)))
    if (points.isEmpty) Seq(-180.0, -90.0, 180.0, 90.0)
    else {
      val (west, east) = (points.map(_._1).min, points.map(_._1).max)
      val (south, north) = (points.map(_._2).min, points.map(_._2).max)
      val padLon = math.max((east - west) * 0.05, 0.01)
      val padLat = math.max((north - south) * 0.05, 0.01)
      Seq(west - padLon, south - padLat, east + padLon, north + padLat)
    }
  }
}

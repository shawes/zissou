package ui

import java.nio.file.{Files, Path}
import org.scalatest.flatspec.AnyFlatSpec
import tools.ShapefileTestUtil

class MapReaderTest extends AnyFlatSpec {

  private val sites = Seq(Site("west", 1, -28.0, 153.0), Site("east", 2, -28.0, 154.0), Site("north", 3, -27.0, 153.5))

  "thin" should "keep everything when there is room, and otherwise spread the picks evenly in order" in {
    assert(MapReader.thin(Seq(1, 2, 3), 5) == Seq(1, 2, 3))
    assert(MapReader.thin(1 to 100, 4) == Seq(1, 26, 51, 76))
    assert(MapReader.thin(1 to 100, 0).isEmpty)
    assert(MapReader.thin(1 to 7, 7) == (1 to 7))
  }

  "simplify" should "keep the first and last point and at most the asked number" in {
    val path = (0 until 100).map(i => Seq(i.toDouble, 0.0))
    val simple = MapReader.simplify(path, 10)
    assert(simple.size == 10 && simple.head == path.head && simple.last == path.last)
    assert(simple.map(_.head) == simple.map(_.head).sorted, "the order of the path is kept")
    assert(MapReader.simplify(path.take(5), 10) == path.take(5))
  }

  "nearestSite" should "pick the closest release site, and -1 when there are none" in {
    assert(MapReader.nearestSite(Seq(153.1, -28.0), sites) == 0)
    assert(MapReader.nearestSite(Seq(153.9, -28.1), sites) == 1)
    assert(MapReader.nearestSite(Seq(153.5, -27.2), sites) == 2)
    assert(MapReader.nearestSite(Seq(0.0, 0.0), Seq.empty) == -1)
  }

  "bounds" should "surround everything drawn, with a margin" in {
    val b = MapReader.bounds(sites, Seq.empty, Seq(Track(0, Seq(Seq(152.5, -29.0), Seq(154.5, -26.0)))))
    assert(b(0) < 152.5 && b(1) < -29.0 && b(2) > 154.5 && b(3) > -26.0)
    assert(b(0) > 152.0 && b(3) < -25.5, "the margin is small")
  }

  it should "not collapse when there is a single point, and cover the world when there is nothing" in {
    val single = MapReader.bounds(Seq(Site("a", 1, -28.0, 153.0)), Seq.empty, Seq.empty)
    assert(single(2) > single(0) && single(3) > single(1))
    assert(MapReader.bounds(Seq.empty, Seq.empty, Seq.empty) == Seq(-180.0, -90.0, 180.0, 90.0))
  }

  // ---- reading real shapefiles -------------------------------------------------

  private def runWith(
      settled: Seq[Option[Seq[(Double, Double)]]],
      sampled: Seq[Option[Seq[(Double, Double)]]] = Seq.empty,
      withHabitat: Boolean = true
  ): (Path, Path) = {
    val base = Files.createTempDirectory("zissou-map-test")
    val run = Files.createDirectories(base.resolve("runs").resolve("20260101-120000-x"))
    val out = Files.createDirectories(run.resolve("output").resolve("config"))
    Files.writeString(out.resolve("x-connectivity-matrix.csv"), "id,born,region,source,age,recruited-date,settle-id\n")
    ShapefileTestUtil.writeLines(out.resolve("x-settled.shp").toFile, settled)
    ShapefileTestUtil.writeLines(out.resolve("x-dispersal.shp").toFile, sampled)
    val habitat = base.resolve("reefs.shp")
    if (withHabitat) {
      ShapefileTestUtil.writeReefs(habitat.toFile, Seq(
        (101, "Reef", Seq((153.65, -28.85), (153.65, -28.75), (153.75, -28.75), (153.75, -28.85))),
        (102, "Other", Seq((153.85, -28.55), (153.85, -28.45), (153.95, -28.45), (153.95, -28.55)))))
    }
    Files.writeString(run.resolve("config.yaml"), SampleConfig.yaml(habitat = habitat.toString))
    (run, base)
  }

  private def line(n: Int, startLon: Double = 153.7, startLat: Double = -28.8): Option[Seq[(Double, Double)]] =
    Some((0 until n).map(i => (startLon + i * 0.001, startLat - i * 0.002)))

  "Reading a run's map" should "return the reefs, the release sites and the larva paths" in {
    val (run, base) = runWith(Seq(line(100), line(50)), Seq(line(30)))
    val map = MapReader.read(run, base, 400).get
    assert(map.sites == Seq(Site("patch 101", 101, -28.8, 153.7)))
    assert(map.reefs.map(r => (r.patch, r.habitat)) == Seq((101, "Reef"), (102, "Other")))
    assert(map.reefs.head.rings.head.size == 5, "a closed ring")
    assert(map.settled.total == 2 && map.settled.tracks.size == 2)
    assert(map.sampled.total == 1)
    assert(map.settled.tracks.head.points.head == Seq(153.7, -28.8) && map.settled.tracks.head.site == 0)
    assert(map.notes.isEmpty)
    assert(map.bounds(0) < 153.65 && map.bounds(2) > 153.95)
  }

  it should "thin and simplify the paths it sends, and say how many there really were" in {
    val (run, base) = runWith((0 until 60).map(_ => line(180)))
    val map = MapReader.read(run, base, 10).get
    assert(map.settled.total == 60 && map.settled.tracks.size == 10)
    assert(map.settled.tracks.forall(_.points.size == 24))
    assert(map.settled.tracks.forall(t => t.points.head == Seq(153.7, -28.8) && t.points.last == Seq(round(153.7 + 179 * 0.001), round(-28.8 - 179 * 0.002))))
  }

  private def round(v: Double) = math.round(v * 10000.0) / 10000.0

  // a polygon of `vertices` corners round a centre
  private def disc(lon: Double, lat: Double, radius: Double, vertices: Int): Seq[(Double, Double)] =
    (0 until vertices).map(i => (lon + radius * math.cos(2 * math.Pi * i / vertices), lat + radius * math.sin(2 * math.Pi * i / vertices)))

  private def mapOfReefs(reefs: Seq[(Int, String, Seq[(Double, Double)])]) = {
    val (run, base) = runWith(Seq(line(10)), Seq.empty, withHabitat = false)
    ShapefileTestUtil.writeReefs(base.resolve("reefs.shp").toFile, reefs)
    MapReader.read(run, base, 400).get
  }

  it should "draw the reefs to the scale of the map: simplified, and without those too small to see" in {
    // a reef round the map, 1 degree across, with 720 corners, and one 100 m across, over a map of about 3 degrees
    val map = mapOfReefs(Seq(
      (1, "Reef", disc(153.5, -28.0, 0.5, 720)),
      (2, "Reef", disc(155.0, -26.0, 0.0005, 8)),
      (3, "Reef", disc(152.0, -27.0, 0.1, 40))))
    assert(map.reefs.map(_.patch) == Seq(1, 3), "the one too small to see is left out, the larger first")
    assert(map.reefs.head.rings.head.size < 150, s"720 corners became ${map.reefs.head.rings.head.size}")
    assert(map.reefs.head.rings.head.size > 8, "but it is still a disc")
    assert(map.notes.exists(n => n.contains("2 of the 3 reefs")), map.notes.toString)
  }

  // a star of `points` spikes, alternating between the radius and half of it: detail that survives simplification
  private def star(lon: Double, lat: Double, radius: Double, points: Int): Seq[(Double, Double)] =
    (0 until 2 * points).map { i =>
      val r = if (i % 2 == 0) radius else radius / 2
      (lon + r * math.cos(math.Pi * i / points), lat + r * math.sin(math.Pi * i / points))
    }

  it should "keep the largest reefs, up to a number of points, when there are very many" in {
    // 19,600 stars of 20 corners on a grid 0.2 degrees across (a pixel is 0.00017 of it, a spike 0.0003): 392,000 points
    val reefs = (0 until 19600).map { i =>
      val radius = 0.0004 + (i % 7) * 0.00005
      (i + 1, "Reef", star(153.0 + (i % 140) * 0.0014, -29.0 + (i / 140) * 0.0014, radius, 10))
    }
    val map = mapOfReefs(reefs)
    val points = map.reefs.map(_.rings.map(_.size).sum).sum
    assert(map.reefs.size < 19600 && map.reefs.size > 3000, s"${map.reefs.size} reefs kept")
    assert(points <= 150000 && points > 100000, s"$points points")
    assert(map.notes.exists(n => n.contains(s"${map.reefs.size} of the 19600 reefs")), map.notes.toString)
    // the largest are the ones kept: the biggest of the seven sizes (patches 7, 14 ...) are all in, the smallest (1, 8 ...) mostly out
    val kept = map.reefs.map(_.patch).toSet
    assert((0 until 19600).filter(i => i % 7 == 6).forall(i => kept.contains(i + 1)))
    assert(kept.count(_ % 7 == 1) < kept.count(_ % 7 == 0))
  }

  it should "say nothing about the reefs when all are drawn" in {
    val map = mapOfReefs(Seq((1, "Reef", disc(153.5, -28.0, 0.1, 12)), (2, "Reef", disc(153.8, -28.0, 0.1, 12))))
    assert(map.reefs.size == 2 && map.notes.isEmpty, map.notes.toString)
  }

  it should "skip records with no geometry, and explain when that is all there is" in {
    val (run, base) = runWith(Seq(None, None, None))
    val map = MapReader.read(run, base, 400).get
    assert(map.settled.total == 0 && map.settled.tracks.isEmpty)
    assert(map.notes.exists(n => n.contains("x-settled.shp") && n.contains("empty")), map.notes.toString)

    val (mixedRun, mixedBase) = runWith(Seq(None, line(10), None, line(10)))
    val mixed = MapReader.read(mixedRun, mixedBase, 400).get
    assert(mixed.settled.total == 2 && mixed.notes.isEmpty)
  }

  it should "still return the paths when the habitat shapefile is missing, with a note" in {
    val (run, base) = runWith(Seq(line(10)), withHabitat = false)
    val map = MapReader.read(run, base, 400).get
    assert(map.reefs.isEmpty && map.settled.total == 1)
    assert(map.notes.exists(_.contains("reefs.shp") && true), map.notes.toString)
  }

  it should "have no map before the run has written results" in {
    val base = Files.createTempDirectory("zissou-map-empty")
    val run = Files.createDirectories(base.resolve("run"))
    Files.writeString(run.resolve("config.yaml"), SampleConfig.yaml())
    assert(MapReader.read(run, base, 400).isEmpty)
  }
}

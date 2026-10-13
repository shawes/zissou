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
    assert(map.reefs.map(r => (r.patch, r.habitat)) == Seq((101, "Reef")), "patch 102 was not released from")
    assert(map.reefs.head.rings.head.size == 5, "a closed ring")
    assert(map.settled.total == 2 && map.settled.tracks.size == 2)
    assert(map.sampled.total == 1)
    assert(map.settled.tracks.head.points.head == Seq(153.7, -28.8) && map.settled.tracks.head.site == 0)
    assert(map.notes.isEmpty)
    // round the release reef and the paths, no further: patch 102's reef, to the east, is not drawn
    assert(map.bounds(0) < 153.65 && map.bounds(2) > 153.75 && map.bounds(2) < 153.95)
  }

  it should "thin and simplify the paths it sends, and say how many there really were" in {
    val (run, base) = runWith((0 until 60).map(_ => line(180)))
    val map = MapReader.read(run, base, 10).get
    assert(map.settled.total == 60 && map.settled.tracks.size == 10)
    assert(map.settled.tracks.forall(_.points.size == 24))
    assert(map.settled.tracks.forall(t => t.points.head == Seq(153.7, -28.8) && t.points.last == Seq(round(153.7 + 179 * 0.001), round(-28.8 - 179 * 0.002))))
  }

  private def round(v: Double) = math.round(v * 10000.0) / 10000.0

  it should "draw only the reefs larvae were released from, however many are in the habitat layer" in {
    val (run, base) = runWith(Seq(line(10)), Seq.empty, withHabitat = false)
    // the configuration releases from patch 101; the layer has thousands of others
    val others = (0 until 3000).map(i => (1000 + i, "Reef", Seq((153.0 + i * 0.001, -29.0), (153.0 + i * 0.001, -28.99), (153.001 + i * 0.001, -28.99), (153.001 + i * 0.001, -29.0))))
    ShapefileTestUtil.writeReefs(base.resolve("reefs.shp").toFile,
      Seq((101, "Reef", Seq((153.65, -28.85), (153.65, -28.75), (153.75, -28.75), (153.75, -28.85)))) ++ others)
    val map = MapReader.read(run, base, 400).get
    assert(map.reefs.map(_.patch) == Seq(101))
    assert(map.notes.isEmpty, map.notes.toString)
    // and the map is about them and the larvae, not the whole layer
    assert(map.bounds(0) > 153.5 && map.bounds(2) < 154.0, map.bounds.toString)
  }

  // ---- the land -------------------------------------------------------------------

  private def landShapefile(): java.nio.file.Path = {
    val dir = Files.createTempDirectory("zissou-land-test")
    // a coast: land west of 153.72 east, from 20 south to 40 south (so it runs well outside the map), with a lake in it,
    // an island east of the map, and, far off, a country the map does not reach
    ShapefileTestUtil.writeReefParts(dir.resolve("land.shp").toFile, Seq(
      (1, "Land", Seq(Seq((140.0, -40.0), (140.0, -20.0), (153.72, -20.0), (153.72, -40.0)))),
      (2, "Land", Seq(Seq((170.0, -40.0), (170.0, -39.0), (171.0, -39.0), (171.0, -40.0))))))
    dir.resolve("land.shp")
  }

  it should "draw the land that is inside the map, clipped to it" in {
    val (run, base) = runWith(Seq(line(100), line(50)), Seq(line(30)))
    val map = MapReader.read(run, base, 400, Some(landShapefile())).get
    assert(map.land.size == 1, "the far country is left out")
    val xs = map.land.head.map(_(0))
    val ys = map.land.head.map(_(1))
    assert(math.abs(xs.max - 153.72) < 1e-6, "the coast is where it is")
    assert(xs.min >= map.bounds(0) - 1e-6 && ys.min >= map.bounds(1) - 1e-6 && ys.max <= map.bounds(3) + 1e-6,
      s"clipped to the map ${map.bounds}: x from ${xs.min}, y ${ys.min} to ${ys.max}")
  }

  it should "draw no land when there is no land file, or the file is missing" in {
    val (run, base) = runWith(Seq(line(100)))
    assert(MapReader.read(run, base, 400).get.land.isEmpty)
    assert(MapReader.read(run, base, 400, Some(base.resolve("no-land.shp"))).get.land.isEmpty)
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

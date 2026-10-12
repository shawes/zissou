package physical.habitat

import java.io.File
import java.nio.file.Files
import io.config.{BufferConfig, HabitatConfig}
import locals.Constants.LightWeightException._
import locals.Reef
import org.scalatest.flatspec.AnyFlatSpec
import physical.GeoCoordinate
import scala.collection.parallel.CollectionConverters._
import scala.util.Random
import tools.ShapefileTestUtil

class HabitatManagerTest extends AnyFlatSpec {

  private val KmPerDegree = 111.2
  private val nothing: (Int, Int, Double) = (NoReefToSettleException, NoReefSensedException, NoSwimmingAngleException)

  private type Patch = (Int, String, Seq[Seq[(Double, Double)]])

  private def manager(reefs: Seq[Patch], settlement: Double = 5.0, olfactory: Double = 10.0): HabitatManager = {
    val file = Files.createTempDirectory("zissou-habitat-test").resolve("reefs.shp").toFile
    ShapefileTestUtil.writeReefParts(file, reefs)
    new HabitatManager(HabitatConfig(file.getPath, BufferConfig(settlement, olfactory)))
  }

  /** A square patch `acrossKm` wide, centred on a position. */
  private def square(patch: Int, lon: Double, lat: Double, acrossKm: Double = 6.0): Patch = {
    val halfLat = acrossKm / 2 / KmPerDegree
    val halfLon = acrossKm / 2 / (KmPerDegree * math.cos(math.toRadians(lat)))
    (patch, "Reef", Seq(Seq((lon - halfLon, lat - halfLat), (lon - halfLon, lat + halfLat), (lon + halfLon, lat + halfLat), (lon + halfLon, lat - halfLat))))
  }

  /** The position `km` from a point, on a compass bearing. */
  private def from(lon: Double, lat: Double, km: Double, bearing: Double): GeoCoordinate =
    new GeoCoordinate(
      lat + km * math.cos(math.toRadians(bearing)) / KmPerDegree,
      lon + km * math.sin(math.toRadians(bearing)) / (KmPerDegree * math.cos(math.toRadians(lat))),
      5.0
    )

  private def degrees(angle: Double): Double = ((angle % 360) + 360) % 360

  // the reef centres of the synthetic dataset: two of them (104 and 107) are alike
  // in latitude times longitude, which is what the old lookup sorted on
  private val centres = Seq((101, 153.7, -28.8), (102, 153.9, -28.5), (104, 154.0, -27.9), (105, 153.6, -27.6), (107, 153.5, -28.0))

  "Finding the habitat" should "settle a larva above a reef whichever side it is on" in {
    val habitat = manager(centres.map { case (patch, lon, lat) => square(patch, lon, lat) })
    for ((patch, lon, lat) <- centres; km <- Seq(0.0, 0.5, 2.0); bearing <- 0 until 360 by 22) {
      val found = habitat.getClosestHabitat(from(lon, lat, km, bearing))
      assert(found == (patch, NoReefSensedException, NoSwimmingAngleException), s"patch $patch, $km km at bearing $bearing: $found")
    }
  }

  it should "settle within the settlement distance of the edge, sense within the olfactory distance, and otherwise do nothing" in {
    val (lon, lat) = (153.7, -28.8)
    val habitat = manager(Seq(square(1, lon, lat)), settlement = 5.0, olfactory = 10.0)
    def eastOfTheEdge(km: Double) = from(lon, lat, 3.0 + km, 90) // the edge is 3 km from the centre

    assert(habitat.getClosestHabitat(eastOfTheEdge(4.8))._1 == 1)
    val (settle, sensed, _) = habitat.getClosestHabitat(eastOfTheEdge(5.2))
    assert(settle == NoReefToSettleException && sensed == 1)
    assert(habitat.getClosestHabitat(eastOfTheEdge(9.8))._2 == 1)
    assert(habitat.getClosestHabitat(eastOfTheEdge(10.2)) == nothing)
  }

  it should "point a sensing larva at the reef" in {
    val (lon, lat) = (153.7, -28.8)
    val habitat = manager(Seq(square(1, lon, lat)))
    // the direction is counter-clockwise from east, in degrees: east of the reef it points west, north of it south
    assert(math.abs(degrees(habitat.getClosestHabitat(from(lon, lat, 3.0 + 7.0, 90))._3) - 180) < 1.0)
    assert(math.abs(degrees(habitat.getClosestHabitat(from(lon, lat, 3.0 + 7.0, 0))._3) - 270) < 1.0)
    assert(math.abs(degrees(habitat.getClosestHabitat(from(lon, lat, 3.0 + 7.0, 180))._3) - 90) < 1.0)
  }

  it should "choose the nearer of two reefs" in {
    // two reefs about 11 km apart, edge to edge; a larva 3 km from the east one and 8 km from the west one
    val west = square(1, 153.50, -28.0)
    val east = square(2, 153.50 + 17.0 / (KmPerDegree * math.cos(math.toRadians(28.0))), -28.0)
    val habitat = manager(Seq(west, east), settlement = 2.0, olfactory = 10.0)
    val larva = from(153.50, -28.0, 3.0 + 8.0, 90)
    val (settle, sensed, direction) = habitat.getClosestHabitat(larva)
    assert(settle == NoReefToSettleException && sensed == 2)
    assert(math.abs(degrees(direction) - 0) < 1.0 || math.abs(degrees(direction) - 360) < 1.0, "the nearer reef is to the east")
  }

  it should "not fail, or invent a reef, away from the reefs" in {
    val habitat = manager(centres.map { case (patch, lon, lat) => square(patch, lon, lat) })
    // north of every reef is where the old lookup threw an exception
    for ((lon, lat) <- Seq((153.7, -26.5), (153.7, -40.0), (140.0, -28.0), (170.0, -28.0), (0.0, 0.0), (-170.0, 80.0))) {
      assert(habitat.getClosestHabitat(new GeoCoordinate(lat, lon, 5.0)) == nothing, s"($lon, $lat)")
    }
  }

  it should "work with no reefs at all" in {
    assert(manager(Seq.empty).getClosestHabitat(new GeoCoordinate(-28.0, 153.0, 5.0)) == nothing)
  }

  it should "settle a larva on the reef when the settlement distance is zero, but only on it" in {
    val (lon, lat) = (153.7, -28.8)
    val habitat = manager(Seq(square(1, lon, lat)), settlement = 0.0, olfactory = 0.0)
    assert(habitat.getClosestHabitat(from(lon, lat, 0.0, 0))._1 == 1)
    assert(habitat.getClosestHabitat(from(lon, lat, 2.9, 90))._1 == 1)
    assert(habitat.getClosestHabitat(from(lon, lat, 3.5, 90)) == nothing)
  }

  it should "find a patch from any of its separate pieces" in {
    // one patch in two pieces about 90 km apart, and a decoy between them
    val piece = square(7, 153.0, -28.0)._3.head
    val farPiece = square(7, 154.0, -28.0)._3.head
    val habitat = manager(Seq((7, "Reef", Seq(piece, farPiece)), square(8, 153.5, -29.5)))
    assert(habitat.getClosestHabitat(from(154.0, -28.0, 1.0, 45))._1 == 7)
    assert(habitat.getClosestHabitat(from(153.0, -28.0, 1.0, 225))._1 == 7)
    assert(habitat.getClosestHabitat(from(153.5, -29.5, 1.0, 0))._1 == 8)
  }

  it should "settle on the reef that comes first in the file when two are equally near" in {
    val overlapping = Seq(square(10, 153.7, -28.8), square(11, 153.7, -28.8))
    assert(manager(overlapping).getClosestHabitat(from(153.7, -28.8, 1.0, 0))._1 == 10)
    assert(manager(overlapping.reverse).getClosestHabitat(from(153.7, -28.8, 1.0, 0))._1 == 11)
  }

  // ---- against measuring every reef --------------------------------------------

  private def randomReefs(random: Random, count: Int, west: Double, east: Double, south: Double, north: Double): Seq[Patch] =
    (0 until count).map { i =>
      val pieces = (0 until (if (i % 7 == 0) 3 else 1)).map { _ =>
        val lon = west + random.nextDouble() * (east - west)
        val lat = south + random.nextDouble() * (north - south)
        val (w, h) = (0.01 + random.nextDouble() * 0.1, 0.01 + random.nextDouble() * 0.1)
        Seq((lon - w, lat - h), (lon - w, lat + h), (lon + w, lat + h * 0.5), (lon + w, lat - h))
      }
      (1000 + i, if (i % 5 == 0) "Other" else "Reef", pieces)
    }

  /** The answer got by measuring the distance to every reef, which cannot miss one. */
  private def bruteForce(reefs: Seq[GeometryAdaptor], settlement: Double, olfactory: Double)(at: GeoCoordinate): (Int, Int, Double) = {
    val (kilometres, position) = reefs.zipWithIndex.map { case (reef, i) => (reef.distance(at), i) }.min
    if (kilometres <= settlement) (reefs(position).id, NoReefSensedException, NoSwimmingAngleException)
    else if (kilometres <= olfactory) (NoReefToSettleException, reefs(position).id, reefs(position).direction(at))
    else nothing
  }

  private def agreesWithBruteForce(seed: Int, region: (Double, Double, Double, Double), settlement: Double, olfactory: Double, points: Int): Unit = {
    val (west, east, south, north) = region
    val random = new Random(seed)
    val patches = randomReefs(random, 60, west, east, south, north)
    val habitat = manager(patches, settlement, olfactory)
    val adaptors = patches.map { case (id, _, pieces) => new GeometryAdaptor(ShapefileTestUtil.multiPolygon(pieces), id, Reef) }
    val oracle = bruteForce(adaptors, settlement, olfactory)

    val vertices = patches.flatMap(_._3.flatten)
    val positions = (0 until points).map { i =>
      if (i % 5 == 0) new GeoCoordinate(south + random.nextDouble() * (north - south), west + random.nextDouble() * (east - west), 5.0)
      else {
        // most of the positions are near a reef, around the distances where the answer changes
        val (lon, lat) = vertices(random.nextInt(vertices.size))
        from(lon, lat, random.nextDouble() * olfactory * 1.6, random.nextDouble() * 360)
      }
    }
    var settled, sensed = 0
    positions.foreach { p =>
      val expected = oracle(p)
      assert(habitat.getClosestHabitat(p) == expected, s"at $p")
      if (expected._1 != NoReefToSettleException) settled += 1 else if (expected._2 != NoReefSensedException) sensed += 1
    }
    // the comparison only means something if it covers settling, sensing and neither
    assert(settled > points / 20 && sensed > points / 20 && settled + sensed < points - points / 20, s"settled $settled, sensed $sensed of $points")
  }

  it should "agree with measuring every reef, off the coast of Queensland" in {
    agreesWithBruteForce(1, (150.0, 158.0, -35.0, -20.0), settlement = 5.0, olfactory = 10.0, points = 1500)
  }

  it should "agree with measuring every reef, across the equator and with long distances" in {
    agreesWithBruteForce(2, (140.0, 150.0, -5.0, 8.0), settlement = 20.0, olfactory = 80.0, points = 1000)
  }

  it should "agree with measuring every reef, near the pole where degrees of longitude are short" in {
    agreesWithBruteForce(3, (-10.0, 10.0, -68.0, -60.0), settlement = 10.0, olfactory = 40.0, points = 1000)
  }

  it should "agree with measuring every reef, with very short distances" in {
    agreesWithBruteForce(4, (150.0, 152.0, -30.0, -28.0), settlement = 0.5, olfactory = 1.0, points = 1000)
  }

  it should "give the same answers when the model's threads all search at once" in {
    val random = new Random(5)
    val habitat = manager(randomReefs(random, 60, 150.0, 158.0, -35.0, -20.0))
    val positions = (0 until 4000).map(_ => new GeoCoordinate(-35.0 + random.nextDouble() * 15, 150.0 + random.nextDouble() * 8, 5.0))
    assert(positions.par.map(habitat.getClosestHabitat).seq == positions.map(habitat.getClosestHabitat))
  }
}

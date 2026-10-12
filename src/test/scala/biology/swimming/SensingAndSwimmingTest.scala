package biology.swimming

import java.nio.file.Files
import io.config.{BufferConfig, HabitatConfig}
import locals.{Directed, StrategyTwo}
import org.scalatest.flatspec.AnyFlatSpec
import physical.GeoCoordinate
import physical.habitat.HabitatManager
import tools.ShapefileTestUtil

/** From sensing a reef to swimming at it: the heading must survive the trip between the two. */
class SensingAndSwimmingTest extends AnyFlatSpec {

  private val KmPerDegree = 111.2
  private val (reefLon, reefLat) = (153.7, -28.8)

  private val habitat = {
    val half = 3.0 / KmPerDegree // a reef 6 km across
    val halfLon = half / math.cos(math.toRadians(reefLat))
    val file = Files.createTempDirectory("zissou-sensing-test").resolve("reef.shp").toFile
    ShapefileTestUtil.writeReefs(file, Seq((1, "Reef",
      Seq((reefLon - halfLon, reefLat - half), (reefLon - halfLon, reefLat + half), (reefLon + halfLon, reefLat + half), (reefLon + halfLon, reefLat - half)))))
    new HabitatManager(HabitatConfig(file.getPath, BufferConfig(5.0, 10.0)))
  }

  private val swimming = new HorizontalSwimming(Directed, StrategyTwo, 0.2, 1, 1, true, 1)

  private def larvaAt(km: Double, bearing: Double) =
    new GeoCoordinate(
      reefLat + km * math.cos(math.toRadians(bearing)) / KmPerDegree,
      reefLon + km * math.sin(math.toRadians(bearing)) / (KmPerDegree * math.cos(math.toRadians(reefLat))),
      5.0)

  "A larva that senses a reef" should "swim towards it, from whichever side it is on" in {
    for (bearing <- 0 until 360 by 15) {
      val larva = larvaAt(3.0 + 7.0, bearing) // 7 km from the edge: it senses the reef but cannot settle
      val (settle, sensed, direction) = habitat.getClosestHabitat(larva)
      assert(settle == locals.Constants.LightWeightException.NoReefToSettleException && sensed == 1, s"bearing $bearing")

      val velocity = swimming(new HorizontalSwimmingVariables(direction, 10, 2, 5, 8, 20)).get
      // which way the reef lies, in kilometres east and north of the larva
      val east = (reefLon - larva.longitude) * KmPerDegree * math.cos(math.toRadians(reefLat))
      val north = (reefLat - larva.latitude) * KmPerDegree
      val cosine = (velocity.u * east + velocity.v * north) / (math.hypot(velocity.u, velocity.v) * math.hypot(east, north))
      val offBy = math.toDegrees(math.acos(math.min(1.0, cosine)))
      assert(offBy < 10.0, f"bearing $bearing: the larva swims $offBy%.0f degrees away from the reef")
    }
  }
}

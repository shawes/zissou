package biology

import com.github.nscala_time.time.Imports._
import org.scalatest._
import physical.GeoCoordinate

import scala.collection.mutable._

class SpawnTest extends flatspec.AnyFlatSpec {

  val spawningLocations = new ListBuffer[SpawningLocation]
  val date = DateTime.now().minusHours(DateTime.now().getHourOfDay)
  val releasePeriod1 = date.minusDays(5) to date.plusDays(1)
  val releasePeriod2 = date.minusDays(2) to date.plusDays(5)

  spawningLocations += new SpawningLocation(
    "lizard island",
    1000,
    202,
    new GeoCoordinate(1, 2),
    releasePeriod1,
    1
  )
  spawningLocations += new SpawningLocation(
    "bare island",
    576,
    199,
    new GeoCoordinate(4, 7),
    releasePeriod2,
    1
  )

  "A spawn" should "be able to construct with no parameters" in {
    val spawn = new Spawn()
    assert(spawn != null)
  }

  it should "also take spawning locations as a constructor" in {
    val spawn = new Spawn(spawningLocations.toList)
    assert(spawn != null)
  }

  it should "workout if its spawning season" in {
    val spawn = new Spawn(spawningLocations.toList)
    val canSpawn = spawn.isItSpawningSeason(date.toLocalDateTime)
    assert(canSpawn)
  }

  it should "return first site where it is spawning" in {
    val spawn = new Spawn(spawningLocations.toList)
    val sites =
      spawn.getSitesWhereFishAreSpawning(date.minusDays(3).toLocalDateTime)
    assert(releasePeriod1.contains(date))
    assert(sites.size == 1)
  }

  it should "return second site where it is spawning" in {
    val spawn = new Spawn(spawningLocations.toList)
    val sites =
      spawn.getSitesWhereFishAreSpawning(date.plusDays(4).toLocalDateTime)
    assert(releasePeriod2.contains(date))
    assert(
      (releasePeriod1.getStart() to date
        .toDateTime(DateTimeZone.UTC)).toPeriod.getDays % 5 == 0
    )
    assert(sites.size == 1)
  }

  it should "return both sites where its spawning" in {
    val spawn = new Spawn(spawningLocations.toList)
    val sites = spawn.getSitesWhereFishAreSpawning(date.toLocalDateTime)
    assert(releasePeriod1.contains(date))
    assert(releasePeriod2.contains(date))
    assert(sites.size == 2)
  }

}

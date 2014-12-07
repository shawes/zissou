package biology

import com.github.nscala_time.time.Imports._
import org.scalatest.FlatSpec
import physical.GeoCoordinate

import scala.collection.mutable._

class SpawnTest extends FlatSpec {

  val spawningLocations = new ArrayBuffer[SpawningLocation]
  val date = DateTime.now.minusHours(DateTime.now.getHourOfDay)

  spawningLocations += new SpawningLocation("lizard island", 1000, new GeoCoordinate(1, 2), new Interval(DateTime.now.minusDays(5), DateTime.now.plusDays(1)), 5)
  spawningLocations += new SpawningLocation("bare island", 576, new GeoCoordinate(4, 7), new Interval(DateTime.now.minusDays(2), DateTime.now.plusDays(5)), 1)

  "A spawn" should "be able to construct with no parameters" in {
    val spawn = new Spawn()
    assert(spawn != null)
  }

  it should "also take spawning locations as a constructor" in {
    val spawn = new Spawn(spawningLocations)
    assert(spawn != null)
  }

  it should "workout if its spawning season" in {
    val spawn = new Spawn(spawningLocations)
    val canSpawn = spawn.isItSpawningSeason(date)
    assert(canSpawn)
  }

  it should "return the site one where its spawning" in {
    val spawn = new Spawn(spawningLocations)
    val sites = spawn.getSitesWhereFishAreSpawning(date.minusDays(3))
    assert(sites.size == 1)
  }

  it should "return the site two where its spawning" in {
    val spawn = new Spawn(spawningLocations)
    val sites = spawn.getSitesWhereFishAreSpawning(date.plusDays(4))
    assert(sites.size == 1)
  }

  it should "return both sites where its spawning" in {
    val spawn = new Spawn(spawningLocations)
    val sites = spawn.getSitesWhereFishAreSpawning(date)
    assert(sites.size == 2)
  }

}

package biology

import com.github.nscala_time.time.Imports._
import org.scalatest.FlatSpec
import physical.GeoCoordinate

import scala.collection.mutable._

class SpawnTest extends FlatSpec {

  val spawningLocations = new ListBuffer[SpawningLocation]
  val date = DateTime.now.minusHours(DateTime.now.getHourOfDay).toLocalDateTime

  spawningLocations += new SpawningLocation("lizard island", 1000, 202,new GeoCoordinate(1, 2), new Interval(DateTime.now.minusDays(5), DateTime.now.plusDays(1)), 4)
  spawningLocations += new SpawningLocation("bare island", 576, 199, new GeoCoordinate(4, 7), new Interval(DateTime.now.minusDays(2), DateTime.now.plusDays(5)), 1)

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
    val canSpawn = spawn.isItSpawningSeason(date)
    assert(canSpawn)
  }

  it should "return the site one where its spawning" in {
    val spawn = new Spawn(spawningLocations.toList)
    val sites = spawn.getSitesWhereFishAreSpawning(date.minusDays(4))
    assert(sites.size == 1)
  }

  it should "return the site two where its spawning" in {
    val spawn = new Spawn(spawningLocations.toList)
    val sites = spawn.getSitesWhereFishAreSpawning(date.plusDays(4))
    assert(sites.size == 1)
  }

  it should "return both sites where its spawning" in {
    val spawn = new Spawn(spawningLocations.toList)
    val sites = spawn.getSitesWhereFishAreSpawning(date)
    assert(sites.size == 2)
  }

}

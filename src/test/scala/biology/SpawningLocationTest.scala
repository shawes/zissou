package biology

import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import org.scalatest.flatspec.AnyFlatSpec
import physical.GeoCoordinate

class SpawningLocationTest extends AnyFlatSpec {

  val spawningLocation = new SpawningLocation("lizard island", 1000, 202,new GeoCoordinate(1, 2),  DateTime.now().minusDays(5) to DateTime.now().plusDays(1), 5)

  "A spawning location" should "be able to construct" in {
    assert(spawningLocation != null)
  }

  it should "return true if it can spawn" in {
    val can = spawningLocation.canSpawn(LocalDateTime.now())
    assert(can)
  }

  it should "return false if it cannot spawn" in {
    val cannot = !spawningLocation.canSpawn(LocalDateTime.now().plusDays(10))
    assert(cannot)
  }

  // the days (counted from the first day of the release period) a site releases on
  private def releaseDays(interval: Int, periodDays: Int): Seq[Int] = {
    val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
    val site = new SpawningLocation("s", 100, 1, new GeoCoordinate(-28, 154), start to start.plusDays(periodDays), interval)
    (0 until periodDays + 5).filter(d => site.timeToSpawn(start.plusDays(d).toLocalDateTime))
  }

  "The release days" should "be every interval days from the first day, for any interval" in {
    for (interval <- 1 to 12) {
      assert(releaseDays(interval, 60) == (0 until 60 by interval), s"interval $interval")
    }
  }

  it should "stay on the interval across weeks and months" in {
    // Period.getDays restarted every week and every month: interval 5 released on 0,5,7,12,14,...
    assert(releaseDays(5, 15) == Seq(0, 5, 10))
    assert(releaseDays(3, 15) == Seq(0, 3, 6, 9, 12))
    // and a weekly release went wrong once a month had passed
    assert(releaseDays(7, 70) == (0 until 70 by 7))
  }

  it should "not release after the period has ended, or before it starts" in {
    val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
    val site = new SpawningLocation("s", 100, 1, new GeoCoordinate(-28, 154), start to start.plusDays(10), 5)
    assert(!site.timeToSpawn(start.minusDays(5).toLocalDateTime))
    assert(!site.timeToSpawn(start.plusDays(10).toLocalDateTime), "the end of the period is not included")
    assert(!site.timeToSpawn(start.plusDays(15).toLocalDateTime))
  }

  // the model asks on the first step of each day (see DailyEventsTest), so the time of day is not Spawn's to judge
  it should "release a spawn site on its release days, through Spawn" in {
    val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
    val spawn = new Spawn(List(new SpawningLocation("s", 100, 1, new GeoCoordinate(-28, 154), start to start.plusDays(10), 5)))
    assert(spawn.getSitesWhereFishAreSpawning(start.plusDays(5).toLocalDateTime).size == 1)
    assert(spawn.getSitesWhereFishAreSpawning(start.plusDays(5).plusHours(5).toLocalDateTime).size == 1)
    assert(spawn.getSitesWhereFishAreSpawning(start.plusDays(7).toLocalDateTime).isEmpty, "day 7 is not a release day for interval 5")
  }
}

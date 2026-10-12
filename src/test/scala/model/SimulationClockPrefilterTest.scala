package model

import java.util.Locale
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import locals.{Constants, Sun, TimeStepType}
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}

/** The approximate pre-filter must never change what the exact calculator says. */
class SimulationClockPrefilterTest extends AnyFlatSpec {

  private val interval =
    new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC) to new DateTime(2002, 12, 31, 0, 0, DateTimeZone.UTC)

  // The sun event straight from the library, with none of the clock's shortcuts
  private def exactSunEvent(
      location: GeoCoordinate,
      now: LocalDateTime,
      stepSeconds: Int
  ): Option[Sun.Value] = {
    val zone = DateTimeZone.forOffsetHours((location.longitude * 24 / 360).toInt)
    val calculator = new SunriseSunsetCalculator(
      new Location(location.latitude, location.longitude), zone.toTimeZone)
    val nowDateTime = now.toDateTime(zone)
    val calendar = nowDateTime.toCalendar(Locale.getDefault())
    def near(event: DateTime): Boolean = {
      val seconds = math.abs((event.getMillis - nowDateTime.getMillis) / 1000)
      seconds <= Constants.DuskOrDawn || seconds < stepSeconds
    }
    if (near(new DateTime(calculator.getOfficialSunriseCalendarForDate(calendar)))) Some(Sun.Rising)
    else if (near(new DateTime(calculator.getOfficialSunsetCalendarForDate(calendar)))) Some(Sun.Setting)
    else None
  }

  "The sun event with the pre-filter" should "match the exact calculator everywhere it is used" in {
    var checked = 0
    var near = 0
    for {
      hours <- Seq(1, 2, 6)
      clock = new SimulationClock(interval, new TimeStep(hours, TimeStepType.Hour))
      lat <- -50 to -10 by 8
      lon <- 142 to 180 by 9
      day <- 1 to 365 by 23
      minute <- 0 until 1440 by 20
    } {
      val place = new GeoCoordinate(lat.toDouble, lon.toDouble, 5.0)
      clock.now = new LocalDateTime(2002, 1, 1, 0, 0).plusDays(day - 1).plusMinutes(minute)
      val expected = exactSunEvent(place, clock.now, hours * 3600)
      assert(clock.sunEvent(place) == expected, s"lat $lat lon $lon at ${clock.now} step ${hours}h")
      checked += 1
      if (expected.isDefined) near += 1
    }
    // the grid must include plenty of both outcomes for the comparison to mean anything
    assert(checked > 50000 && near > 5000 && near < checked - 5000)
  }
}

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

  // The sun event straight from the library, with none of the clock's shortcuts, for a
  // time on the clock, which is UTC
  private def exactSunEvent(
      location: GeoCoordinate,
      now: LocalDateTime,
      stepSeconds: Int
  ): Option[Sun.Value] = {
    val zone = DateTimeZone.forOffsetHours((location.longitude * 24 / 360).toInt)
    val calculator = new SunriseSunsetCalculator(
      new Location(location.latitude, location.longitude), zone.toTimeZone)
    val nowDateTime = now.toDateTime(DateTimeZone.UTC).withZone(zone)
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
      lon <- Seq(-150, -75, 0, 142, 151, 160, 169, 178)
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

  it should "match the exact calculator minute by minute around the cut-off" in {
    // the shortcuts only trust the approximation away from the cut-off, so the
    // times around it, where the exact calculator decides, must still agree
    var checked = 0
    for {
      hours <- Seq(1, 2, 6)
      clock = new SimulationClock(interval, new TimeStep(hours, TimeStepType.Hour))
      (lat, lon) <- Seq((-28.0, 154.0), (-45.0, 146.0), (-12.0, 175.0), (-33.0, 151.0))
      day <- Seq(5, 80, 172, 266, 355)
      place = new GeoCoordinate(lat, lon, 5.0)
      start = new LocalDateTime(2002, 1, 1, 0, 0).plusDays(day - 1)
      minute <- 0 until 1440 by 2
    } {
      clock.now = start.plusMinutes(minute)
      assert(clock.sunEvent(place) == exactSunEvent(place, clock.now, hours * 3600),
        s"lat $lat lon $lon at ${clock.now} step ${hours}h")
      checked += 1
    }
    assert(checked > 40000)
  }
}

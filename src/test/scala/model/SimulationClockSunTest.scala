package model

import java.util.Locale
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import locals.{Sun, TimeStepType}
import org.scalatest.flatspec.AnyFlatSpec
import physical.{GeoCoordinate, TimeStep}

class SimulationClockSunTest extends AnyFlatSpec {

  private val place = new GeoCoordinate(-28.0, 154.0, 5.0)
  private val interval =
    new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC) to new DateTime(2002, 2, 20, 0, 0, DateTimeZone.UTC)

  private def clockWithStepOf(hours: Int) =
    new SimulationClock(interval, new TimeStep(hours, TimeStepType.Hour))

  // The calculator's own sunrise and sunset on the test day, as times on the simulation
  // clock, which is UTC: about 05:00 local is 19:00 UTC the day before
  private val timezone = DateTimeZone.forOffsetHours(10)
  private val calculator =
    new SunriseSunsetCalculator(new Location(place.latitude, place.longitude), timezone.toTimeZone)
  private val noon = new DateTime(2002, 1, 5, 12, 0, timezone).toCalendar(Locale.getDefault())
  private def onTheClock(calendar: java.util.Calendar): LocalDateTime =
    new DateTime(calendar).withZone(DateTimeZone.UTC).toLocalDateTime
  private val sunrise: LocalDateTime = onTheClock(calculator.getOfficialSunriseCalendarForDate(noon))
  private val sunset: LocalDateTime = onTheClock(calculator.getOfficialSunsetCalendarForDate(noon))
  // local noon and midnight on the test day, on the clock
  private val localNoon = new LocalDateTime(2002, 1, 5, 2, 0)
  private val localMidnight = new LocalDateTime(2002, 1, 4, 14, 0)

  private def at(clock: SimulationClock, time: LocalDateTime): Option[Sun.Value] = {
    clock.now = time
    clock.sunEvent(place)
  }

  "The simulation clock" should "report sunrise when the time is at sunrise" in {
    assert(at(clockWithStepOf(1), sunrise) == Some(Sun.Rising))
  }

  it should "report sunset when the time is at sunset" in {
    assert(at(clockWithStepOf(1), sunset) == Some(Sun.Setting))
  }

  it should "report sunrise shortly before and after it" in {
    val clock = clockWithStepOf(1)
    assert(at(clock, sunrise.minusMinutes(30)) == Some(Sun.Rising))
    assert(at(clock, sunrise.plusMinutes(30)) == Some(Sun.Rising))
  }

  it should "report nothing at midday" in {
    assert(at(clockWithStepOf(1), localNoon) == None)
  }

  it should "report nothing at midnight" in {
    assert(at(clockWithStepOf(1), localMidnight) == None)
  }

  it should "take its time as UTC, not as the local time" in {
    assert(sunrise.getHourOfDay == 18 || sunrise.getHourOfDay == 19)
    assert(sunset.getHourOfDay == 8 || sunset.getHourOfDay == 9)
    // the local times of sunrise and sunset, read as UTC, are mid-afternoon and the next
    // dawn at the larva, which is what the clock used to take for dawn and dusk
    val sunriseReadAsUtc = new DateTime(calculator.getOfficialSunriseCalendarForDate(noon)).withZone(timezone).toLocalDateTime
    val sunsetReadAsUtc = new DateTime(calculator.getOfficialSunsetCalendarForDate(noon)).withZone(timezone).toLocalDateTime
    assert(at(clockWithStepOf(1), sunriseReadAsUtc) == None)
    assert(at(clockWithStepOf(1), sunsetReadAsUtc) == Some(Sun.Rising))
  }

  it should "find dawn and dusk at the larva's own longitude" in {
    // the same latitude on the Greenwich meridian, where local time is UTC, and at 150 west
    for (longitude <- Seq(0.0, -150.0)) {
      val there = new GeoCoordinate(place.latitude, longitude, 5.0)
      val zone = DateTimeZone.forOffsetHours((longitude * 24 / 360).toInt)
      val calc = new SunriseSunsetCalculator(new Location(there.latitude, there.longitude), zone.toTimeZone)
      val day = new DateTime(2002, 1, 5, 12, 0, zone).toCalendar(Locale.getDefault())
      val clock = clockWithStepOf(1)
      clock.now = onTheClock(calc.getOfficialSunriseCalendarForDate(day))
      assert(clock.sunEvent(there) == Some(Sun.Rising), s"sunrise at $longitude")
      clock.now = onTheClock(calc.getOfficialSunsetCalendarForDate(day))
      assert(clock.sunEvent(there) == Some(Sun.Setting), s"sunset at $longitude")
    }
  }

  it should "report nothing a few hours after sunrise" in {
    assert(at(clockWithStepOf(1), sunrise.plusHours(3)) == None)
  }

  it should "treat a time step longer than the dusk window as the window" in {
    // three hours from sunrise is outside the 45 minute window but inside a 6 hour step
    assert(at(clockWithStepOf(6), sunrise.plusHours(3)) == Some(Sun.Rising))
    assert(at(clockWithStepOf(2), sunrise.plusHours(3)) == None)
  }
}

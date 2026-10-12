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

  // The calculator's own sunrise and sunset, as local wall-clock times on the test day
  private val timezone = DateTimeZone.forOffsetHours(10)
  private val calculator =
    new SunriseSunsetCalculator(new Location(place.latitude, place.longitude), timezone.toTimeZone)
  private val noon = new DateTime(2002, 1, 5, 12, 0, timezone).toCalendar(Locale.getDefault())
  private val sunrise: LocalDateTime =
    new DateTime(calculator.getOfficialSunriseCalendarForDate(noon)).toLocalDateTime
  private val sunset: LocalDateTime =
    new DateTime(calculator.getOfficialSunsetCalendarForDate(noon)).toLocalDateTime

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
    assert(at(clockWithStepOf(1), new LocalDateTime(2002, 1, 5, 12, 0)) == None)
  }

  it should "report nothing at midnight" in {
    assert(at(clockWithStepOf(1), new LocalDateTime(2002, 1, 5, 0, 0)) == None)
  }

  it should "report nothing a few hours after sunrise" in {
    assert(at(clockWithStepOf(1), sunrise.plusHours(3)) == None)
  }

  it should "treat a time step longer than the dusk window as the window" in {
    // three hours from sunrise is outside the 45 minute window but inside a 6 hour step
    assert(at(clockWithStepOf(6), sunrise.plusHours(3)) == Some(Sun.Rising))
    assert(at(clockWithStepOf(2), sunrise.plusHours(3)) == None)
  }

  it should "agree between the single-event checks and the combined one" in {
    val clock = clockWithStepOf(1)
    clock.now = sunset
    assert(!clock.isSunRising(place) && clock.isSunSetting(place))
    clock.now = sunrise
    assert(clock.isSunRising(place) && !clock.isSunSetting(place))
  }
}

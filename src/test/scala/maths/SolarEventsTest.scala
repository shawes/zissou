package maths

import java.util.Locale
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import org.scalatest.flatspec.AnyFlatSpec

class SolarEventsTest extends AnyFlatSpec {

  "The approximate solar events" should "stay within 5 minutes of the exact calculator" in {
    var worst = 0.0
    for (lat <- -50 to -10 by 5; lon <- 142 to 180 by 6; day <- 1 to 365 by 17) {
      val offset = (lon * 24 / 360.0).toInt
      val zone = DateTimeZone.forOffsetHours(offset)
      val noon = new DateTime(2002, 1, 1, 12, 0, zone).plusDays(day - 1).toCalendar(Locale.getDefault())
      val calculator = new SunriseSunsetCalculator(new Location(lat.toDouble, lon.toDouble), zone.toTimeZone)
      val sunrise = new DateTime(calculator.getOfficialSunriseCalendarForDate(noon)).getMillisOfDay / 1000.0
      val sunset = new DateTime(calculator.getOfficialSunsetCalendarForDate(noon)).getMillisOfDay / 1000.0
      val (approxRise, approxSet) = SolarEvents.approximate(lat, lon, day, offset).get
      worst = math.max(worst, SolarEvents.distanceAroundTheClock(approxRise, sunrise))
      worst = math.max(worst, SolarEvents.distanceAroundTheClock(approxSet, sunset))
    }
    assert(worst < 300, s"worst error was $worst seconds")
  }

  it should "report no events where the sun does not rise or set" in {
    // within the Antarctic circle at midsummer
    assert(SolarEvents.approximate(-80.0, 0.0, 355, 0).isEmpty)
  }

  "The distance around the clock" should "go the short way round midnight" in {
    assert(SolarEvents.distanceAroundTheClock(23 * 3600, 1 * 3600) == 2 * 3600)
    assert(SolarEvents.distanceAroundTheClock(1 * 3600, 23 * 3600) == 2 * 3600)
    assert(SolarEvents.distanceAroundTheClock(6 * 3600, 8 * 3600) == 2 * 3600)
  }
}

package model

import grizzled.slf4j.Logging
import physical.TimeStep
import locals.Constants
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.Locale
import locals.Sun
import locals.Sun._


class SimulationClock(interval: Interval, val timeStep: TimeStep) extends Logging {

  val start = interval.getStart
  val end = interval.getEnd
  //val totalDays: Int = interval.toPeriod.getDays
  var now: LocalDateTime = start.toLocalDateTime()


  def tick(): Unit = {
    now = now + timeStep.totalSeconds.seconds
    debug(s"clock is $now")
  }

  def stillTime: Boolean = now.toDateTime(DateTimeZone.UTC) < end
  def isMidnight: Boolean = now.getHourOfDay == 0

  def isSunSetting(location : GeoCoordinate) : Boolean = {
    new SunTimes(location).isNear(Sun.Setting)
  }

  def isSunRising(location : GeoCoordinate) : Boolean = {
    new SunTimes(location).isNear(Sun.Rising)
  }

  /** Which sun event, if any, the location is near, sharing one calculator for both checks. */
  def sunEvent(location : GeoCoordinate) : Option[Sun] = {
    val sunTimes = new SunTimes(location)
    if (sunTimes.isNear(Sun.Rising)) Some(Sun.Rising)
    else if (sunTimes.isNear(Sun.Setting)) Some(Sun.Setting)
    else None
  }

  // Building the calculator is the expensive part, so it is made once per location
  private class SunTimes(location : GeoCoordinate) {
    private val timezone = getTimeZone(location)
    private val calculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timezone.toTimeZone)
    private val nowDateTime : DateTime = now.toDateTime(timezone)

    def isNear(sun : Sun) : Boolean = {
      val sunsetOrSunrise = sun match {
        case Sun.Setting => new DateTime(calculator.getOfficialSunsetCalendarForDate(
        nowDateTime.toCalendar(Locale.getDefault())))
        case Sun.Rising => new DateTime(calculator.getOfficialSunriseCalendarForDate(nowDateTime.toCalendar(Locale.getDefault())))
      }

      // The whole number of seconds between now and the event. Period.getSeconds
      // is only the 0-59 seconds field, which made every time look like dawn.
      val timeToSunsetOrSunrise =
        math.abs((sunsetOrSunrise.getMillis - nowDateTime.getMillis) / 1000)

      timeToSunsetOrSunrise <= Constants.DuskOrDawn || timeToSunsetOrSunrise < timeStep.totalSeconds
    }
  }

  def getTimeZone(location : GeoCoordinate) : DateTimeZone = {
    val offset : Int = (location.longitude * 24 / 360).toInt
    DateTimeZone.forOffsetHours(offset)
  }

}

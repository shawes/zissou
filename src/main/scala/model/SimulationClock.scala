package model

import grizzled.slf4j.Logging
import physical.TimeStep
import locals.Constants
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.Locale
import locals._

class SimulationClock(interval: Interval, val timeStep: TimeStep)
    extends Logging {

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

  def isSunSetting(location: GeoCoordinate): Boolean = {
    isSunSettingOrRising(location, Setting)
  }

  def isSunRising(location: GeoCoordinate): Boolean = {
    isSunSettingOrRising(location, Rising)
  }

  private def isSunSettingOrRising(
      location: GeoCoordinate,
      sun: SunDirection
  ): Boolean = {
    val localTimeZone = getTimeZone(location)
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(
      new Location(location.latitude, location.longitude),
      localTimeZone.toTimeZone
    )

    val nowDateTime: DateTime = now.toDateTime(localTimeZone)

    val sunsetOrSunrise = sun match {
      case Setting =>
        new DateTime(
          sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(
            nowDateTime.toCalendar(Locale.getDefault())
          )
        )
      case Rising =>
        new DateTime(
          sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(
            nowDateTime.toCalendar(Locale.getDefault())
          )
        )
    }

    val timeToSunsetOrSunrise = nowDateTime.isBefore(sunsetOrSunrise) match {
      case true  => (nowDateTime to sunsetOrSunrise).toPeriod.getSeconds()
      case false => (sunsetOrSunrise to nowDateTime).toPeriod.getSeconds()
    }

    timeToSunsetOrSunrise <= Constants.DuskOrDawn || timeToSunsetOrSunrise < timeStep.totalSeconds
  }

  def getTimeZone(location: GeoCoordinate): DateTimeZone = {
    val offset: Int = (location.longitude * 24 / 360).toInt
    DateTimeZone.forOffsetHours(offset)
  }

}

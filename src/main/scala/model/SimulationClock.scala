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
  val totalDays: Int = interval.toPeriod.getDays
  var now: DateTime = start


  def tick(): Unit = {
    now = now + timeStep.totalSeconds.seconds
  }

  def stillTime: Boolean = now.isBefore(end)
  def isMidnight: Boolean = now.getHourOfDay == 0

  def isSunSetting(location : GeoCoordinate, timeZone : String) : Boolean = {
    isSunSettingOrRising(location, timeZone, Sun.Setting)
  }

  def isSunRising(location : GeoCoordinate, timeZone : String) : Boolean = {
    isSunSettingOrRising(location, timeZone, Sun.Rising)
  }

  private def isSunSettingOrRising(location : GeoCoordinate, timeZone : String, sun : Sun) : Boolean = {
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone)

    val sunsetOrSunrise = sun match {
      case Sun.Setting => new DateTime(sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(
      now.toCalendar(Locale.getDefault())))
      case Sun.Rising => new DateTime(sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(now.toCalendar(Locale.getDefault())))
    }

    val timeToSunsetOrSunrise = now.isBefore(sunsetOrSunrise) match {
      case true => (now to sunsetOrSunrise).toPeriod.getSeconds()
      case false => (sunsetOrSunrise to now).toPeriod.getSeconds()
    }
    timeToSunsetOrSunrise <= Constants.DuskOrDawn || timeToSunsetOrSunrise < timeStep.totalSeconds
  }

  private def getTimeZone(location : GeoCoordinate) : Double = {
    location.longitude * 24 / 360
  }

}

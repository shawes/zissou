package main

import grizzled.slf4j.Logging
import physical.TimeStep
import locals.Constants
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.joda.time.Seconds


class SimulationClock(interval: Interval, val timeStep: TimeStep) extends Logging {

  val start = interval.getStart
  val end = interval.getEnd
  val totalDays: Int = Days.daysBetween(start, end).getDays
  var now: DateTime = start


  def tick(): Unit = {
    now = now.plusSeconds(timeStep.totalSeconds)
  }

  def stillTime: Boolean = now.isBefore(end)
  def isMidnight: Boolean = now.getHourOfDay == 0

  def isSunSetting(location : GeoCoordinate, timeZone : String) : Boolean = {
    debug("check if sun is setting")
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone)
    val sunset = new DateTime(sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(
      now.toCalendar(Locale.getDefault())))
    val timeToSunset = Math.abs(Seconds.secondsBetween(now, sunset).getSeconds())
    debug("Now is " + now + ", sunset is " + sunset)
    debug("Time to sunset is: " + timeToSunset)
    if(timeToSunset <= Constants.DuskOrDawn || timeToSunset < timeStep.totalSeconds) {
      debug("Time to diurnally migrate!")
      return true
    }
    return false
  }

  def isSunRising(location : GeoCoordinate, timeZone : String) : Boolean = {
    debug("check if sun is rising")
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone)
    val sunrise = new DateTime(sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(now.toCalendar(Locale.getDefault())))
    val timeToSunrise = Math.abs(Seconds.secondsBetween(now, sunrise).getSeconds())
        val sunriseTimeZone = sunrise.toDateTime(DateTimeZone.forID(timeZone))
    debug("Now is " + now + ", sunrise is " + sunriseTimeZone)
    debug("Time to sunrise is: " + timeToSunrise)
    if(timeToSunrise <= Constants.DuskOrDawn || timeToSunrise < timeStep.totalSeconds) {
      return true
    }
    return false
  }

}

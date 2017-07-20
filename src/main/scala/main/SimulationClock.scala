package main

import physical.TimeStep
import locals.Constants
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.Calendar
import org.joda.time.Seconds


class SimulationClock(interval: Interval, val timeStep: TimeStep) {

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
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone)
    val sunset = new DateTime(sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(Calendar.getInstance()))
    val timeToSunset = Seconds.secondsBetween(now, sunset).getSeconds()
    if(timeToSunset <= Constants.DuskOrDawn || timeToSunset < timeStep.totalSeconds) {
      return true
    }
    return false
  }

  def isSunRising(location : GeoCoordinate, timeZone : String) : Boolean = {
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone)
    val sunrise = new DateTime(sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(Calendar.getInstance()))
    val timeToSunrise = Seconds.secondsBetween(now, sunrise).getSeconds()
    if(timeToSunrise <= Constants.DuskOrDawn || timeToSunrise < timeStep.totalSeconds) {
      return true
    }
    return false
  }

}

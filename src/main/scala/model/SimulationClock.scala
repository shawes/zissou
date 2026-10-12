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
import maths.SolarEvents


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

  // The approximation is within 2 minutes of the exact calculator over the supported
  // latitudes and longitudes, so a quarter of an hour either side of the cut-off leaves
  // a wide safety factor. Only times inside that band need the exact calculator.
  private val ApproximationMargin = 900
  // A time is near an event when it is within this many seconds of it: the dusk
  // window, or anything shorter than a whole time step
  private val NearThreshold = math.max(Constants.DuskOrDawn, timeStep.totalSeconds - 1)
  private val SkipBeyondSeconds = NearThreshold + 1 + ApproximationMargin
  private val SecondsInDay = 86400

  // The exact calculator is expensive, so it is only built, and asked, when the
  // approximation cannot settle the answer
  private class SunTimes(location : GeoCoordinate) {
    private val timezone = getTimeZone(location)
    private lazy val calculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timezone.toTimeZone)
    private val nowDateTime : DateTime = now.toDateTime(timezone)
    private lazy val approximateEvents = SolarEvents.approximate(
      location.latitude, location.longitude, now.getDayOfYear, timezone.getOffset(nowDateTime) / 3600000)

    def isNear(sun : Sun) : Boolean = approximatelyNear(sun) match {
      case Some(answer) => answer
      case None => isNearExactly(sun)
    }

    // Some(answer) if the approximation is far enough from the cut-off to be certain
    private def approximatelyNear(sun : Sun) : Option[Boolean] = approximateEvents match {
      case Some((sunrise, sunset)) =>
        val event = if (sun == Sun.Rising) sunrise else sunset
        val nowSeconds = now.getMillisOfDay / 1000.0
        if (SolarEvents.distanceAroundTheClock(nowSeconds, event) > SkipBeyondSeconds) Some(false)
        // an event too close to midnight might belong to the neighbouring day, so leave it
        else if (event > ApproximationMargin && event < SecondsInDay - ApproximationMargin &&
          math.abs(nowSeconds - event) < NearThreshold - ApproximationMargin) Some(true)
        else None
      case None => None
    }

    private def isNearExactly(sun : Sun) : Boolean = {
      val sunsetOrSunrise = sun match {
        case Sun.Setting => new DateTime(calculator.getOfficialSunsetCalendarForDate(
        nowDateTime.toCalendar(Locale.getDefault())))
        case Sun.Rising => new DateTime(calculator.getOfficialSunriseCalendarForDate(nowDateTime.toCalendar(Locale.getDefault())))
      }

      // The whole number of seconds between now and the event. Period.getSeconds
      // is only the 0-59 seconds field, which made every time look like dawn.
      val timeToSunsetOrSunrise =
        math.abs((sunsetOrSunrise.getMillis - nowDateTime.getMillis) / 1000)

      timeToSunsetOrSunrise <= NearThreshold
    }
  }

  def getTimeZone(location : GeoCoordinate) : DateTimeZone = {
    val offset : Int = (location.longitude * 24 / 360).toInt
    DateTimeZone.forOffsetHours(offset)
  }

}

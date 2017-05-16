package biology

import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.Calendar
import org.joda.time.Seconds
import maths.{ContinuousRange, RandomNumberGenerator}

class VerticalMigrationDiel(val probabilities : List[VerticalMigrationDielProbability]) {

  private def duskOrDawn = 2700 //Dusk or dawn roughly starts / ends 45 mins before or after sunset / sunrise

  def getDepth(location : GeoCoordinate, currentTime : DateTime, timeZone : DateTimeZone, timeStep : Double) : Double = {
    val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone.toTimeZone)
    val sunset = new DateTime(
      sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(Calendar.getInstance()))
    val sunrise = new DateTime(
      sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(Calendar.getInstance()))

    if(isSunSetting(sunset, currentTime, timeStep)) {
        return getRandomDepth(day = false)
    } else if(isSunRising(sunrise, currentTime, timeStep : Double)) {
        return getRandomDepth(day = true)
    }
    return -1
  }

  private def isSunSetting(sunset : DateTime, currentTime: DateTime, timeStep : Double) : Boolean = {
    val timeToSunset = Seconds.secondsBetween(currentTime, sunset).getSeconds()
    if(timeToSunset <= duskOrDawn || timeToSunset < timeStep) {
      return true
    }
    return false
  }

  private def isSunRising(sunrise : DateTime, currentTime: DateTime, timeStep : Double) : Boolean = {
    val timeToSunrise = Seconds.secondsBetween(currentTime, sunrise).getSeconds()
    if(timeToSunrise <= duskOrDawn || timeToSunrise < timeStep) {
      return true
    }
    return false

  }

  private def getRandomDepth(day : Boolean) : Double = {
    var cumulativeProb = 0.0
    val number = RandomNumberGenerator.get
    val iterator = probabilities.iterator
    var currentDepth: (ContinuousRange, Double) = new Tuple2(new ContinuousRange(), 0)
    val prob = iterator.next

    if(day) {
      currentDepth = (prob.depth, prob.day)
    } else {
      currentDepth = (prob.depth, prob.night)
    }

    cumulativeProb += currentDepth._2
    while (number > cumulativeProb && iterator.hasNext) {
      val prob = iterator.next
          if(day) {
            currentDepth = (prob.depth, prob.day)
          } else {
            currentDepth = (prob.depth, prob.night)
          }
      cumulativeProb += currentDepth._2
    }
    calculateDepthInRange(currentDepth._1)
  }

  private def calculateDepthInRange(depthRange: ContinuousRange): Double = {
    RandomNumberGenerator.get(depthRange.start, depthRange.end)
  }


}

package biology

import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import java.util.Calendar
import org.joda.time.Seconds
import maths.{ContinuousRange, RandomNumberGenerator}
import scala.collection.mutable.ListBuffer

class VerticalMigrationDiel(val probabilities : List[VerticalMigrationProbability]) {

  private def duskOrDawn = 2700 //Dusk or dawn roughly starts / ends 45 mins before or after sunset / sunrise

  def getDepth(location : GeoCoordinate, currentTime : DateTime, timeZone : DateTimeZone, timeStep : Double) : Double = {
      val sunriseSunsetCalculator = new SunriseSunsetCalculator(new Location(location.latitude, location.longitude), timeZone.toTimeZone)
      val sunset = new DateTime(
        sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(Calendar.getInstance()))
      val sunrise = new DateTime(
        sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(Calendar.getInstance()))

      if(isSunSetting(sunset, currentTime, timeStep)) {
          1.0
      } else if(isSunRising(sunrise, currentTime, timeStep : Double)) {
          1.0
      }
      1.0


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

  private def getRandomDepth() : Double = {
      //val list: ListBuffer[(ContinuousRange, Double)] = ListBuffer.empty[(ContinuousRange, Double)]

      var cumulativeProb = 0.0
      val number = RandomNumberGenerator.get

      val iterator = probabilities.iterator
      var currentDepth: (ContinuousRange, Double) = new Tuple2(new ContinuousRange(), 0)
      val prob = iterator.next
      currentDepth = (prob.depth, prob.diel)
      cumulativeProb += currentDepth._2
      while (number > cumulativeProb && iterator.hasNext) {
        val prob = iterator.next
        currentDepth = (prob.depth, prob.diel)
        cumulativeProb += currentDepth._2
      }
      calculateDepthInRange(currentDepth._1)
    }

      private def calculateDepthInRange(depthRange: ContinuousRange): Double = {
        RandomNumberGenerator.get(depthRange.start, depthRange.end)
      }


}

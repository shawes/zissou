package biology

import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import physical.GeoCoordinate

class SpawningLocation(val title: String,
                       val numberOfLarvae: Int,
                       val reefId : Int,
                       val location: GeoCoordinate,
                       val releasePeriod: Interval,
                       val interval: Int) {

  def canSpawn(date: LocalDateTime): Boolean = releasePeriod.contains(date.toDateTime(DateTimeZone.UTC))

  // Release every `interval` days, counting from the first day of the release period.
  // The whole number of days is needed: Period.getDays is only the days field, which
  // starts again every week and every month, so it released on the wrong days for any
  // interval other than 1 and 7.
  def timeToSpawn(date : LocalDateTime) : Boolean = {
    releasePeriod.contains(date.toDateTime(DateTimeZone.UTC)) &&
    (Days.daysBetween(releasePeriod.getStart().toLocalDate, date.toLocalDate).getDays % interval == 0)
  }

  override def toString : String = s"Site: $title, with $numberOfLarvae larvae"
}

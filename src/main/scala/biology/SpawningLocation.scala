package biology

import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate

class SpawningLocation(val title: String,
                       val numberOfLarvae: Int,
                       val reefId : Int,
                       val location: GeoCoordinate,
                       val releasePeriod: Interval,
                       val interval: Int) {

  def canSpawn(date: LocalDateTime): Boolean = releasePeriod.contains(date.toDateTime(DateTimeZone.UTC))

  def timeToSpawn(date : LocalDateTime) : Boolean = {
    releasePeriod.contains(date.toDateTime(DateTimeZone.UTC)) &&
    ((releasePeriod.getStart() to date.toDateTime(DateTimeZone.UTC)).toPeriod.getDays
    % interval == 0)
  }

  override def toString : String = s"Site: $title, with $numberOfLarvae larvae"
}

package biology

import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate

class SpawningLocation(val title: String,
                       val numberOfLarvae: Int,
                       val location: GeoCoordinate,
                       val releasePeriod: Interval,
                       val interval: Int) {
  def canSpawn(date: DateTime): Boolean = {
    releasePeriod.contains(date) && ((releasePeriod.getStart() to date).toDuration().getStandardDays() % interval == 0)
  }

  override def toString : String = s"Site: $title, with $numberOfLarvae larvae"
}

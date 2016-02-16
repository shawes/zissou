package biology

import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate

class SpawningLocation(val title: String,
                       val numberOfLarvae: Int,
                       val location: GeoCoordinate,
                       val releasePeriod: Interval,
                       val interval: Int) {

  def canSpawn(time: DateTime): Boolean = {
    releasePeriod.contains(time)
  }

  override def toString : String = "Site: " + title + ", with " + numberOfLarvae + " larvae"

}

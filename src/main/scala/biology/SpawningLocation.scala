package biology

import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._

class SpawningLocation(val title: String,
                       val numberOfLarvae: Int,
                       val site: GeoCoordinate,
                       val releasePeriod: Interval) {

  def CanSpawn(time: DateTime): Boolean = {
    releasePeriod.contains(time)
  }

}

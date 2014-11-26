package biology

import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate

class SpawningLocation(val title: String,
                       val numberOfLarvae: Int,
                       val site: GeoCoordinate,
                       val releasePeriod: Interval,
                       val interval: Int) {

  def CanSpawn(time: DateTime): Boolean = {
    releasePeriod.contains(time)
  }

}

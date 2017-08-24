package biology

import com.github.nscala_time.time.Imports._

class Spawn(val spawningLocations: List[SpawningLocation]) {
  def this() = this(List.empty[SpawningLocation])

  def getSitesWhereFishAreSpawning(date: DateTime): List[SpawningLocation] = {
    date.getHourOfDay match {
      case 0 => spawningLocations.filter(x => x.canSpawn(date))
      case _ => List.empty[SpawningLocation]
    }
  }

  def isItSpawningSeason(date: DateTime): Boolean = {
    spawningLocations.count(x => x.canSpawn(date)) > 0
  }
}

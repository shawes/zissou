package biology

import com.github.nscala_time.time.Imports._

class Spawn(val spawningLocations: List[SpawningLocation]) {
  def this() = this(List.empty[SpawningLocation])

  def getSitesWhereFishAreSpawning(date: LocalDateTime): List[SpawningLocation] = {
    date.getHourOfDay match {
      case 0 => spawningLocations.filter(x => x.timeToSpawn(date))
      case _ => List.empty[SpawningLocation]
    }
  }

  def isItSpawningSeason(date: LocalDateTime): Boolean = {
    spawningLocations.count(x => x.canSpawn(date)) > 0
  }
}

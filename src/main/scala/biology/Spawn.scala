package biology

import com.github.nscala_time.time.Imports._

class Spawn(val spawningLocations: List[SpawningLocation]) {
  def this() = this(List.empty[SpawningLocation])

  /** The sites that release larvae on the day of this time. */
  def getSitesWhereFishAreSpawning(date: LocalDateTime): List[SpawningLocation] =
    spawningLocations.filter(x => x.timeToSpawn(date))

  def isItSpawningSeason(date: LocalDateTime): Boolean = {
    spawningLocations.count(x => x.canSpawn(date)) > 0
  }
}

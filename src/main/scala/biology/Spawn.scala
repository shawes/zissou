package biology

import com.github.nscala_time.time.Imports._

class Spawn(val spawningLocations: List[SpawningLocation]) {
  def this() = this(List.empty[SpawningLocation])

  def getSitesWhereFishAreSpawning(date: DateTime): List[SpawningLocation] = {
    // Spawns at midnight
    if (date.getHourOfDay == 0) {
      spawningLocations.filter(x => x.canSpawn(date))
    } else {
      List.empty[SpawningLocation]
    }
  }

  def isItSpawningSeason(date: DateTime): Boolean = {
    spawningLocations.count(x => x.canSpawn(date)) > 0

  }
}

//object Spawn {
//  def apply(spawningLocationsJavaList: java.util.List[SpawningLocation]) = new Spawn(spawningLocationsJavaList)
//}

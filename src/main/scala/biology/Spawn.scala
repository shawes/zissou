package biology

import com.github.nscala_time.time.Imports._

class Spawn(val spawningLocations: Vector[SpawningLocation]) {
  def this() = this(Vector.empty[SpawningLocation])

  def GetSitesWhereFishAreSpawning(date: DateTime): Vector[SpawningLocation] = {
    // only spawns at midnight
    //if(date.hourOfDay()!=0) return List[SpawningLocation]

    spawningLocations.filter(x => x.CanSpawn(date))

  }

  def IsItSpawningSeason(date: DateTime): Boolean = {
    spawningLocations.count(x => x.CanSpawn(date)) > 0
  }
}

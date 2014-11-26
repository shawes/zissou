package biology


import com.github.nscala_time.time.Imports._

import scala.collection.mutable

//import scala.collection.mutable.ArrayBuffer

class Spawn(val spawningLocations: mutable.Buffer[SpawningLocation]) {
  def this() = this(mutable.Buffer.empty[SpawningLocation])

  def getSitesWhereFishAreSpawning(date: DateTime): mutable.Buffer[SpawningLocation] = {
    // only spawns at midnight
    //if(date.hourOfDay()!=0) return List[SpawningLocation]

    spawningLocations.filter(x => x.canSpawn(date))

  }

  def isItSpawningSeason(date: DateTime): Boolean = {
    spawningLocations.count(x => x.canSpawn(date)) > 0

  }
}

//object Spawn {
//  def apply(spawningLocationsJavaList: java.util.List[SpawningLocation]) = new Spawn(spawningLocationsJavaList)
//}

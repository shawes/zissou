package biology


import com.github.nscala_time.time.Imports._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

//import scala.collection.mutable.ArrayBuffer

class Spawn(val spawningLocations: mutable.Buffer[SpawningLocation]) {
  def this() = this(mutable.Buffer.empty[SpawningLocation])

  def getSitesWhereFishAreSpawning(date: DateTime): mutable.Buffer[SpawningLocation] = {

    var spawningSites = new ArrayBuffer[SpawningLocation].toBuffer

    // only spawns at midnight
    if (date.getHourOfDay == 0) {
      spawningSites = spawningLocations.filter(x => x.canSpawn(date))
    }

    spawningSites

  }

  def isItSpawningSeason(date: DateTime): Boolean = {
    spawningLocations.count(x => x.canSpawn(date)) > 0

  }
}

//object Spawn {
//  def apply(spawningLocationsJavaList: java.util.List[SpawningLocation]) = new Spawn(spawningLocationsJavaList)
//}

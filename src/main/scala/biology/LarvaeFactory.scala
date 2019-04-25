package biology

import locals.LarvaType
import locals.LarvaType.LarvaType
import com.github.nscala_time.time.Imports._
import biology.fish.FishSpawner
import biology.fish.FishConfig

trait LarvaeFactory {
    def create(site: SpawningLocation, time: LocalDateTime) : Array[Larva]
}

object LarvaeFactory {
  def apply(s: LarvaType, config : FishConfig) = s match {
      case LarvaType.Fish => new FishSpawner(config, false)
      case _ => null
    }
  }

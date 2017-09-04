package biology

import locals.LarvaType
import locals.LarvaType.LarvaType
import com.github.nscala_time.time.Imports._
import biology.fish.FishFactory
import biology.fish.FishParameters

trait LarvaFactory {
    def create(site: SpawningLocation, time: LocalDateTime) : Array[Larva]
}

object LarvaFactory {
  def apply(s: LarvaType, fish : FishParameters ): LarvaFactory = {
    s match {
      case LarvaType.Fish => new FishFactory(fish, false)
      case _ => null
    }
  }
}

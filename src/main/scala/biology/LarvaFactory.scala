package biology

import locals.LarvaType
import locals.LarvaType.LarvaType
import com.github.nscala_time.time.Imports._
import io.config.FishConfig
import biology.fish.FishFactory

trait LarvaFactory {
    def create(site: SpawningLocation, time: DateTime) : Array[Larva]
}

object LarvaFactory {
  def apply(s: LarvaType, f:FishConfig ): LarvaFactory = {
    s match {
      case LarvaType.Fish => new FishFactory(f,false)
      case _ => null
    }
  }
}

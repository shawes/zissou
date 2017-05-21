package biology

import locals.LarvaType
import locals.LarvaType.LarvaType
import org.joda.time.DateTime
import io.config.FishConfig
import biology.fish.FishFactory

trait LarvaFactory {
    def create(site: SpawningLocation, time: DateTime) : List[Larva]
}

object LarvaFactory {
  def apply(s: LarvaType, f:FishConfig ): LarvaFactory = {
    s match {
      case LarvaType.Fish => new FishFactory(f,false)
      case _ => null
    }
  }
}

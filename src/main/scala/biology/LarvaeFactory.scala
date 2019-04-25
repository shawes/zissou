package biology

import locals._
import com.github.nscala_time.time.Imports._
import biology.fish.FishFactory
import io.config.LarvaConfig

trait LarvaeFactory {
  def create(site: SpawningLocation, time: LocalDateTime): Larva
}

object LarvaeFactory {
  def apply(s: LarvaType, config: LarvaConfig) = s match {
    case Fish => new FishFactory(config)
    case _    => null
  }
}

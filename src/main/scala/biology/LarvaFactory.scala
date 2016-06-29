package biology

import locals.LarvaType
import locals.LarvaType.LarvaType
import org.joda.time.DateTime

object LarvaFactory {
  def apply(s: LarvaType, id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int,
            birthplace: Birthplace, spawned: DateTime, ontogeny: ReefFishOntogeny, migration: VerticalMigration): ReefFish = {
    s match {
      case LarvaType.ReefFish => new ReefFish(id, pelagicLarvalDuration, maximumLifeSpan, birthplace, spawned, ontogeny, migration)
      case _ => null
    }
  }


}

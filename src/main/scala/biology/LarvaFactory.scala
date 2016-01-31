package biology

import locals.PelagicLarvaeState.PelagicLarvaeState
import org.joda.time.DateTime

trait LarvaFactory {
  def createReefFish(id: Int,
                     pelagicLarvalDuration: Int,
                     maximumLifeSpan: Int,
                     birthplace: Birthplace,
                     state: PelagicLarvaeState,
                     spawned: DateTime,
                     ontogeny: Ontogeny,
                     migration: VerticalMigration): Larva
}

package biology

import org.joda.time.DateTime

trait LarvaFactory {
  def createReefFish(id: Int,
                     pelagicLarvalDuration: Int,
                     maximumLifeSpan: Int,
                     birthplace: Birthplace,
                     spawned: DateTime,
                     ontogeny: Ontogeny,
                     migration: VerticalMigration): Larva
}

package biology

import org.joda.time.DateTime

class LarvaConcreteFactory extends LarvaFactory {
  def createReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int,
                     birthplace: Birthplace, spawned: DateTime, ontogeny: Ontogeny, migration: VerticalMigration): ReefFish
  = new ReefFish(id, pelagicLarvalDuration, maximumLifeSpan, birthplace, spawned, ontogeny, migration)


}


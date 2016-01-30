package biology

import locals.PelagicLarvaeState.PelagicLarvaeState
import org.joda.time.DateTime

class LarvaConcreteFactory extends LarvaFactory {
  def createReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int,
                     birthplace: Birthplace, state: PelagicLarvaeState, spawned: DateTime): ReefFish
  = new ReefFish(id, pelagicLarvalDuration, maximumLifeSpan, birthplace, state, spawned)


}


package biology

import locals.PelagicLarvaeState.PelagicLarvaeState

class LarvaConcreteFactory extends LarvaFactory {
  def createReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int,
                     birthplace: Birthplace, state: PelagicLarvaeState): Larva
  = new ReefFish(id, pelagicLarvalDuration, maximumLifeSpan, birthplace, state)


}


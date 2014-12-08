package biology

import locals.PelagicLarvaeState.PelagicLarvaeState

class LarvaeConcreteFactory extends LarvaeFactory {
  def createReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int,
                     birthplace: Birthplace, state: PelagicLarvaeState): MarineLarvae
  = new ReefFish(id, pelagicLarvalDuration, maximumLifeSpan, birthplace, state)


}


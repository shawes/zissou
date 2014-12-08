package biology

import locals.PelagicLarvaeState.PelagicLarvaeState

trait LarvaFactory {
  def createReefFish(id: Int,
                     pelagicLarvalDuration: Int,
                     maximumLifeSpan: Int,
                     birthplace: Birthplace,
                     state: PelagicLarvaeState): Larva
}

package biology

import locals.PelagicLarvaeState.PelagicLarvaeState

trait LarvaeFactory {
  def createReefFish(id: Int,
                     pelagicLarvalDuration: Int,
                     maximumLifeSpan: Int,
                     birthplace: Birthplace,
                     state: PelagicLarvaeState): MarineLarvae
}

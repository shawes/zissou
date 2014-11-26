package biology

class ReefFishFactory extends LarvaeFactory {
  def createReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int): MarineLarvae
  = new ReefFish(id, pelagicLarvalDuration, maximumLifeSpan)


}


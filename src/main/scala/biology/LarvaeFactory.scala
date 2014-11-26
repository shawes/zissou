package biology

trait LarvaeFactory {
  def createReefFish(id: Int, pelagicLarvalDuration: Int, maximumLifeSpan: Int): MarineLarvae
}

package biology

import io.config.FishConfig
import maths.NormalDistribution
import locals.Constants

class TheMaker(parameters: FishConfig, save: Boolean) {
  val distribution = new NormalDistribution(parameters.pelagicLarvalDuration.mean,
    parameters.pelagicLarvalDuration.stdev)
  var larvaeCount: Int = 0
  val spawningFish: ReefFishFactory = new ReefFishFactory()

  def create(sites: List[SpawningLocation]): Vector[Array[MarineLarvae]] = {
    val larvae = Vector.empty

    for (site <- sites) {
      val larvaeAtSite = new Array[MarineLarvae](Constants.LarvaeCapacityAtSite)
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = distribution.getValue

        larvaeAtSite(i) = spawningFish.createReefFish(larvaeCount, convertDaysToSeconds(pld), 0)
      }
      larvae +: larvaeAtSite
    }
    larvae
  }

  private def convertDaysToSeconds(days: Double) = (days * 86400).toInt


}

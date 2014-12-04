package biology

import io.Logger
import io.config.FishConfig
import maths.NormalDistribution

import scala.collection.mutable.ListBuffer

class TheMaker(parameters: FishConfig, save: Boolean) {
  val distribution = new NormalDistribution(parameters.pelagicLarvalDuration.mean,
    parameters.pelagicLarvalDuration.stdev)
  val spawningFish: ReefFishFactory = new ReefFishFactory
  var larvaeCount: Int = 0

  def create(sites: List[SpawningLocation]): List[List[MarineLarvae]] = {
    val larvae: ListBuffer[List[MarineLarvae]] = ListBuffer.empty

    for (site <- sites) {
      Logger.info("Site found is " + site.toString)
      val larvaeAtSite = new ListBuffer[MarineLarvae]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = distribution.getValue
        larvaeAtSite += spawningFish.createReefFish(larvaeCount, convertDaysToSeconds(pld), 0)
        //Logger.info(larvaeAtSite(i).toString)
      }
      larvae += larvaeAtSite.toList
      Logger.info("Larvae size is now: " + larvaeCount)
    }
    larvae.toList
  }

  private def convertDaysToSeconds(days: Double) = (days * 86400).toInt


}

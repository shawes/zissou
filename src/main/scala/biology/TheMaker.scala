package biology

import io.config.FishConfig
import maths.NormalDistribution
import org.clapper.avsl.Logger

import scala.collection.mutable.ListBuffer

class TheMaker(parameters: FishConfig, save: Boolean) {
  val distribution = new NormalDistribution(parameters.pelagicLarvalDuration.mean,
    parameters.pelagicLarvalDuration.stdev)
  val spawningFish: ReefFishFactory = new ReefFishFactory
  val logger = Logger(classOf[TheMaker])
  var larvaeCount: Int = 0

  def create(sites: List[SpawningLocation]): List[List[MarineLarvae]] = {
    val larvae: ListBuffer[List[MarineLarvae]] = ListBuffer.empty

    for (site <- sites) {
      logger.debug("Site found is " + site.toString)
      val larvaeAtSite = new ListBuffer[MarineLarvae]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = distribution.getValue
        larvaeAtSite += spawningFish.createReefFish(larvaeCount, convertDaysToSeconds(pld), 0)
        //Logger.info(larvaeAtSite(i).toString)
      }
      larvae += larvaeAtSite.toList
      logger.debug("Larvae size is now: " + larvaeCount)
    }
    larvae.toList
  }

  private def convertDaysToSeconds(days: Double) = (days * 86400).toInt


}

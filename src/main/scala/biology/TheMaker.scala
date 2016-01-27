package biology

import grizzled.slf4j._
import io.config.PelagicLarvalDurationConfig
import locals.PelagicLarvaeState
import org.apache.commons.math3.distribution.NormalDistribution

import scala.collection.mutable.ListBuffer

class TheMaker(pld: PelagicLarvalDurationConfig, save: Boolean) {
  val distribution = new NormalDistribution(pld.mean, pld.stdev)
  val spawningFish: LarvaConcreteFactory = new LarvaConcreteFactory
  val logger = Logger(classOf[TheMaker])
  var larvaeCount: Int = 0


  def create(sites: List[SpawningLocation]): List[List[Larva]] = {
    val larvae: ListBuffer[List[Larva]] = ListBuffer.empty

    for (site <- sites) {
      logger.debug("Site found is " + site.toString)
      val larvaeAtSite = new ListBuffer[Larva]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = distribution.sample
        logger.debug("The pld is " + pld)
        larvaeAtSite += spawningFish.createReefFish(larvaeCount, convertDaysToSeconds(pld), 0,
          new Birthplace(site.title, site.site), PelagicLarvaeState.Pelagic)
        //Logger.info(larvaeAtSite(i).toString)
      }
      larvae += larvaeAtSite.toList
      logger.debug("Larvae size is now: " + larvaeCount)
    }
    larvae.toList
  }

  private def convertDaysToSeconds(days: Double) = (days * 86400).toInt


}

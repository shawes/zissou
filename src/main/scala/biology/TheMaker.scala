package biology

import grizzled.slf4j._
import io.config.PelagicLarvalDurationConfig
import locals.PelagicLarvaeState
import org.apache.commons.math3.distribution.NormalDistribution
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer

class TheMaker(pld: PelagicLarvalDurationConfig, save: Boolean) extends Logging {
  val distribution = new NormalDistribution(pld.mean, pld.stdev)
  val spawningFish: LarvaConcreteFactory = new LarvaConcreteFactory
  var larvaeCount: Int = 0


  def create(sites: List[SpawningLocation], time: DateTime): List[List[Larva]] = {
    val larvae: ListBuffer[List[Larva]] = ListBuffer.empty

    for (site <- sites) {
      debug("Site found is " + site.toString)
      val larvaeAtSite = new ListBuffer[Larva]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = distribution.sample
        debug("The pld is " + pld)
        larvaeAtSite += spawningFish.createReefFish(larvaeCount, convertDaysToSeconds(pld), 0,
          new Birthplace(site.title, site.site), PelagicLarvaeState.Pelagic, time)
        //Logger.info(larvaeAtSite(i).toString)
      }
      larvae += larvaeAtSite.toList
      debug("Larvae size is now: " + larvaeCount)
    }
    larvae.toList
  }

  private def convertDaysToSeconds(days: Double) = (days * 86400).toInt


}

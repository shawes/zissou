package biology

import grizzled.slf4j._
import io.config.ConfigMappings._
import io.config.FishConfig
import locals.Constants
import org.apache.commons.math3.distribution.NormalDistribution
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer

class FishFactory(fish: FishConfig, save: Boolean) extends Logging {
  val distribution = new NormalDistribution(fish.pelagicLarvalDuration.mean, fish.pelagicLarvalDuration.stdev)
  val spawningFish: LarvaConcreteFactory = new LarvaConcreteFactory
  var larvaeCount: Int = 0


  def createReefFish(sites: List[SpawningLocation], time: DateTime): List[List[ReefFish]] = {
    val larvae: ListBuffer[List[ReefFish]] = ListBuffer.empty

    for (site <- sites) {
      debug("Site found is " + site.toString)
      val larvaeAtSite = new ListBuffer[ReefFish]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = distribution.sample
        debug("The pld is " + pld)
        larvaeAtSite append spawningFish.createReefFish(larvaeCount, convertDaysToSeconds(pld), convertDaysToSeconds(pld),
          new Birthplace(site.title, site.location), time, fish.ontogeny, fish.verticalMigrationProbabilities)
      }
      larvae append larvaeAtSite.toList
      debug("Larvae size is now: " + larvaeCount)
    }
    larvae.toList
  }

  private def convertDaysToSeconds(days: Double) = (days * Constants.SecondsInDay).toInt


}

package biology

import grizzled.slf4j._
import io.config.ConfigMappings._
import io.config.FishConfig
import locals.{Constants, LarvaType}
import maths.{RandomNumberGenerator, Time}
import org.apache.commons.math3.distribution.NormalDistribution
import org.joda.time.DateTime
import physical.GeoCoordinate

import scala.collection.mutable.ListBuffer

class ReefFishFactory(fish: FishConfig, save: Boolean, random: RandomNumberGenerator) extends Logging {
  val pldDistribution = new NormalDistribution(fish.pelagicLarvalDuration.mean, fish.pelagicLarvalDuration.stdev)
  val preflexionDistribution = new NormalDistribution(fish.ontogeny.preFlexion, Constants.SecondsInDay * 0.5)
  val flexionDistribution = new NormalDistribution(fish.ontogeny.flexion, Constants.SecondsInDay * 0.75)
  val postFlexionDistribution = new NormalDistribution(fish.ontogeny.postFlexion, Constants.SecondsInDay * 1.0)
  var larvaeCount: Int = 0


  def createReefFish(sites: List[SpawningLocation], time: DateTime): List[List[ReefFish]] = {
    val larvae: ListBuffer[List[ReefFish]] = ListBuffer.empty
    for (site <- sites) {
      debug("Site found is " + site.toString)
      val larvaeAtSite = new ListBuffer[ReefFish]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = pldDistribution.sample

        val birthLoc = new GeoCoordinate(site.location.latitude + random.getPlusMinus * Constants.MaxLatitudeShift,
          site.location.longitude + random.getPlusMinus * Constants.MaxLongitudeShift)
        debug("The pld is " + pld)
        larvaeAtSite append LarvaFactory.apply(LarvaType.ReefFish, larvaeCount,
          Time.convertDaysToSeconds(pld),
          Time.convertDaysToSeconds(pld),
          new Birthplace(site.title, birthLoc),
          time,
          new ReefFishOntogeny(preflexionDistribution.sample.toInt, flexionDistribution.sample().toInt, postFlexionDistribution.sample().toInt),
          fish.verticalMigrationProbabilities)
      }
      larvae append larvaeAtSite.toList
      debug("Larvae size is now: " + larvaeCount)
    }
    larvae.toList
  }


}

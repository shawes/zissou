package biology.fish

import grizzled.slf4j._
import io.config.ConfigMappings._
import io.config.FishConfig
import locals.{Constants, LarvaType}
import maths.{RandomNumberGenerator, Time}
import org.apache.commons.math3.distribution.NormalDistribution
import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate
import biology._

import scala.collection.mutable.ArrayBuffer

class FishFactory(fish: FishConfig, save: Boolean) extends LarvaFactory with Logging {
  val pldDistribution = new NormalDistribution(fish.pelagicLarvalDuration.mean, fish.pelagicLarvalDuration.stdev)
   val preFlexionDistribution = new NormalDistribution(Time.convertDaysToSeconds(fish.ontogeny.preFlexion), Constants.SecondsInDay *0.5)
   val flexionDistribution = new NormalDistribution(Time.convertDaysToSeconds(fish.ontogeny.flexion), Constants.SecondsInDay * 0.5)
   val postFlexionDistribution = new NormalDistribution(Time.convertDaysToSeconds(fish.ontogeny.postFlexion), Constants.SecondsInDay *0.5)
  var larvaeCount: Int = 0

  //debug("VERT: " + fish.verticalMigrationOntogeneticProbabilities.size)


  override def create(site: SpawningLocation, time: DateTime): Array[Larva] = {
    val larvae: ArrayBuffer[Fish] = ArrayBuffer.empty
    //for (site <- sites) {

    // val larvaeAtSite = new ListBuffer[ReefFish]
      for (i <- 0 until site.numberOfLarvae) {
        larvaeCount += 1
        val pld: Double = pldDistribution.sample

        // val birthLoc = new GeoCoordinate(site.location.latitude + RandomNumberGenerator.getPlusMinus * Constants.MaxLatitudeShift,
        //   site.location.longitude + RandomNumberGenerator.getPlusMinus * Constants.MaxLongitudeShift, site.location.depth)

          val birthLoc = new GeoCoordinate(site.location.latitude,site.location.longitude ,site.location.depth)

         val larvalFish = new Fish(larvaeCount,
          Time.convertDaysToSeconds(pld),
          Time.convertDaysToSeconds(pld),
          new Birthplace(site.title, birthLoc),
          time,
          new FishOntogeny(preFlexionDistribution.sample().toInt,     flexionDistribution.sample().toInt,
          postFlexionDistribution.sample().toInt),
          fish.swimming,
          fish.verticalMigrationOntogeneticProbabilities,
          fish.verticalMigrationDielProbabilities)

          larvae += larvalFish
                  // debug("The ontogeny is " + larvalFish.ontogeny.preFlexion + ", " + larvalFish.ontogeny.flexion + ", " + larvalFish.ontogeny.postFlexion)
      }
    //larvae append larvaeAtSite.toList
    //debug("Larvae size is now: " + larvaeCount)
    //}
    larvae.toArray
  }


}

package biology.fish

import grizzled.slf4j._
import io.config.ConfigMappings._
import io.config.FishConfig
import locals.{Constants, LarvaType, PelagicLarvalDurationType, Random, Fixed}
import maths.{RandomNumberGenerator}
import org.apache.commons.math3.distribution.NormalDistribution
import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate
import biology._
import utilities.Time
import scala.collection.mutable.ArrayBuffer

class FishFactory(fishParams: FishParameters, save: Boolean) extends LarvaFactory with Logging {

  val pldDistribution = fishParams.pld.distribution
  val preflexionDistribution = new NormalDistribution(Time.convertDaysToSeconds(fishParams.ontogeny.preflexion), Constants.SecondsInDay *0.5)
  val flexionDistribution = new NormalDistribution(Time.convertDaysToSeconds(fishParams.ontogeny.flexion), Constants.SecondsInDay * 0.5)
  val postflexionDistribution = new NormalDistribution(Time.convertDaysToSeconds(fishParams.ontogeny.postflexion), Constants.SecondsInDay *0.5)

  var larvaeCount: Int = 0

  override def create(site: SpawningLocation, time: DateTime): Array[Larva] = {
    val larvae: ArrayBuffer[Fish] = ArrayBuffer.empty

    for (i <- 0 until site.numberOfLarvae) {
      larvaeCount += 1

      val pld: Double = pldDistribution.sample
      // Handles case of demersal eggs
      val preflexion = fishParams.ontogeny.preflexion match {
        case 0 => 0
        case _ => preflexionDistribution.sample().toInt
      }
      val flexion = flexionDistribution.sample().toInt
      val postflexion = postflexionDistribution.sample().toInt

      val birthLoc = new GeoCoordinate(site.location.latitude +       RandomNumberGenerator.getPlusMinus * Constants.MaxLatitudeShift,
        site.location.longitude + RandomNumberGenerator.getPlusMinus * Constants.MaxLongitudeShift, site.location.depth)


      def getNonSettlementPeriod() : Double = {
        val settlement = fishParams.pld.nonSettlementPeriod
        if(settlement < pld) settlement
        else pld
      }

      val nonSettlementPeriod : Double = fishParams.pld.pelagicLarvalDurationType match {
        case Random => getNonSettlementPeriod()
        case Fixed => pld
      }


      val larvalFish = new Fish(larvaeCount,
                                Time.convertDaysToSeconds(pld),
                                Time.convertDaysToSeconds(pld),
                                new Birthplace(site.title, birthLoc),
                                time,
                                new FishOntogeny(preflexion, flexion, postflexion),
                                fishParams.swimming,
                                fishParams.verticalMigrationOntogeneticProbabilities,
                                fishParams.verticalMigrationDielProbabilities,
                                Time.convertDaysToSeconds(nonSettlementPeriod))
      larvae += larvalFish
    }
    larvae.toArray
  }
}

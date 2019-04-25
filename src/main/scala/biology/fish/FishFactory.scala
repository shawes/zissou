package biology.fish

import grizzled.slf4j._
import io.config.ConfigMappings._
import io.config.LarvaConfig
import locals.{Constants, LarvaType, Random, Fixed, DistributionType}
import maths.{RandomNumberGenerator}
import org.apache.commons.math3.distribution.NormalDistribution
import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate
import biology._
import utilities.Time
import scala.collection.mutable.ArrayBuffer
import java.util.UUID.randomUUID

class FishFactory(config: LarvaConfig) extends LarvaeFactory with Logging {

  val pld = new PelagicLarvalDuration(
    new NormalDistribution(
      config.pelagicLarvalDuration.mean,
      config.pelagicLarvalDuration.stdev
    ),
    config.pelagicLarvalDuration.pldType match {
      case "fixed" => true
      case _       => false
    },
    config.pelagicLarvalDuration.nonSettlementPeriod
  )

  val hatchingDistribution = new NormalDistribution(
    Time.convertDaysToSeconds(config.ontogeny.hatching),
    Constants.SecondsInDay * 0.5
  )

  val preflexionDistribution = new NormalDistribution(
    Time.convertDaysToSeconds(config.ontogeny.preflexion),
    Constants.SecondsInDay * 0.5
  )
  val flexionDistribution = new NormalDistribution(
    Time.convertDaysToSeconds(config.ontogeny.flexion),
    Constants.SecondsInDay * 0.5
  )
  val postflexionDistribution = new NormalDistribution(
    Time.convertDaysToSeconds(config.ontogeny.postflexion),
    Constants.SecondsInDay * 0.5
  )

  var larvaeCount: Int = 0

  def create(site: SpawningLocation, time: LocalDateTime): Larva = {

    val fishPld: Double =
      if (pld.isFixed) pld.distribution.getMean else pld.distribution.sample

    // Handles case of demersal eggs
    val hatching = hatchingDistribution.sample().toInt
    val preflexion = config.ontogeny.preflexion match {
      case 0 => 0
      case _ => preflexionDistribution.sample().toInt
    }
    val flexion = flexionDistribution.sample().toInt
    val postflexion = postflexionDistribution.sample().toInt
    val birthLocation = new GeoCoordinate(
      site.location.latitude,
      site.location.longitude,
      RandomNumberGenerator.get(0, site.location.depth)
    )

    def getNonSettlementPeriod(): Double = {
      val settlement = pld.nonSettlementPeriod
      if (settlement < fishPld) {
        settlement
      } else {
        fishPld
      }
    }

    val nonSettlementPeriod: Double =
      if (pld.isFixed) fishPld else getNonSettlementPeriod()

    val fish = new Fish(
      randomUUID.toString(),
      Time.convertDaysToSeconds(fishPld),
      Time.convertDaysToSeconds(fishPld),
      new Birthplace(site.title, site.reefId, birthLocation),
      time,
      hatching,
      preflexion,
      flexion,
      postflexion,
      config.verticalMigrationOntogeneticProbabilities,
      config.verticalMigrationDielProbabilities,
      Time.convertDaysToSeconds(nonSettlementPeriod)
    )
    fish
  }
}

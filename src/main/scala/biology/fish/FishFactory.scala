package biology.fish

import grizzled.slf4j._
import io.config.ConfigMappings._
import io.config.LarvaConfig
import locals._
import maths.{RandomNumberGenerator}
import org.apache.commons.math3.distribution.NormalDistribution
import com.github.nscala_time.time.Imports._
import physical.GeoCoordinate
import biology._
import biology.swimming._
import utilities.Time
import scala.collection.mutable.ArrayBuffer
import java.util.UUID

class FishFactory(config: LarvaConfig) extends LarvaeFactory with Logging {

  val pldDistribution = new PelagicLarvalDuration(config.pelagicLarvalDuration)

  val horizontalSwimming = config.swimming match {
    case Some(swim) => {
      Some(
        new HorizontalSwimming(
          swim.ability.getOrElse("") match {
            case "directed"   => Directed
            case "undirected" => Undirected
            case _            => Passive
          },
          swim.strategy match {
            case "one"   => StrategyOne
            case "two"   => StrategyTwo
            case "three" => StrategyThree
          },
          swim.criticalSwimmingSpeed.getOrElse(0),
          swim.inSituSwimmingPotential.getOrElse(1),
          swim.endurance.getOrElse(1),
          swim.reynoldsEffect.getOrElse(false),
          swim.hatchSwimmingSpeed.getOrElse(0)
        )
      )
    }
    case None => None

  }

  val dielMigration = config.dielProbabilities match {
    case Some(dvm) => Some(new DielMigration(dvm.depths, dvm.day, dvm.night))
    case None      => None
  }

  val ovmMigration = config.ovmProbabilities match {
    case Some(ovm) =>
      Some(new OntogeneticMigration(config.ovmProbabilities.get))
    case None => None
  }

  val preflexionDistribution = new NormalDistribution(
    RandomNumberGenerator.generator,
    Time.convertDaysToSeconds(config.ontogeny.preflexion),
    Constants.SecondsInDay * 0.5
  )
  val flexionDistribution = new NormalDistribution(
    RandomNumberGenerator.generator,
    Time.convertDaysToSeconds(config.ontogeny.flexion),
    Constants.SecondsInDay * 0.5
  )
  val postflexionDistribution = new NormalDistribution(
    RandomNumberGenerator.generator,
    Time.convertDaysToSeconds(config.ontogeny.postflexion),
    Constants.SecondsInDay * 0.5
  )

  var larvaeCount: Int = 0

  // A UUID-shaped id that depends only on the seed and creation order, so
  // repeated runs label their larvae identically
  private def deterministicId(): String =
    UUID
      .nameUUIDFromBytes(s"${RandomNumberGenerator.seed}-$larvaeCount".getBytes)
      .toString

  def create(site: SpawningLocation, time: LocalDateTime): Larva = {

    val pld = pldDistribution.getPld()

    // The ages of the stages are drawn independently, so on their own they can come out of
    // order, or before the larva is born. Each stage begins no earlier than the one before it.
    val preflexion = config.ontogeny.preflexion match {
      case 0 => 0
      case _ => math.max(0, preflexionDistribution.sample().toInt)
    }
    val flexion = math.max(preflexion, flexionDistribution.sample().toInt)
    val postflexion = math.max(flexion, postflexionDistribution.sample().toInt)
    val birthLocation = new GeoCoordinate(
      site.location.latitude,
      site.location.longitude,
      RandomNumberGenerator.get(0, site.location.depth)
    )

    // in seconds, as the PLD is, and no longer than it
    val nonSettlementPeriod = math.min(
      Time.convertDaysToSeconds(config.pelagicLarvalDuration.nonSettlementPeriod),
      pld
    )

    larvaeCount += 1
    val fish = new Fish(
      deterministicId(),
      pld,
      pld,
      new Birthplace(site.title, site.reefId, birthLocation),
      time,
      preflexion,
      flexion,
      postflexion,
      ovmMigration,
      dielMigration,
      horizontalSwimming,
      nonSettlementPeriod
    )
    //info("Just created the fish: " + fish)
    fish
  }
}

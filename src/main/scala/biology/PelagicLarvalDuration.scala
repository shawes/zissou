package biology

import org.apache.commons.math3.distribution.NormalDistribution
import locals.DistributionType.DistributionType
import locals.PelagicLarvalDurationType
import io.config._
import utilities.Time
import locals.Constants
import maths.RandomNumberGenerator

class PelagicLarvalDuration(val config: PelagicLarvalDurationConfig) {
  require(config.mean > 0, s"The mean pelagic larval duration must be more than 0 days, not ${config.mean}")

  private val mean = Time.convertDaysToSeconds(config.mean)

  // Only built for a PLD that is sampled: a fixed one needs no standard deviation, and
  // the distribution refuses one of 0
  private lazy val distribution: NormalDistribution =
    new NormalDistribution(
      RandomNumberGenerator.generator,
      mean,
      Constants.SecondsInDay * config.stdev
    )

  def getPld(): Int = config.pldType match {
    case "fixed"                => mean
    case _ if config.stdev == 0 => mean
    case _                      => sample()
  }

  // The normal distribution has no lower bound, so a PLD is drawn again until it is longer
  // than nothing: a larva must live for some time
  private def sample(): Int = {
    var pld = 0
    while (pld <= 0) pld = distribution.sample().toInt
    pld
  }

}

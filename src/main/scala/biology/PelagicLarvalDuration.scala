package biology

import org.apache.commons.math3.distribution.NormalDistribution
import locals._
import locals.Enums.PelagicLarvalDurationType
import io.config._
import utilities.Time
import locals.Constants

class PelagicLarvalDuration(val config: PelagicLarvalDurationConfig) {
  private val distribution: NormalDistribution =
    new NormalDistribution(
      Time.convertDaysToSeconds(config.mean),
      Constants.SecondsInDay * config.stdev
    )

  def getPld(): Int = config.pldType match {
    case "fixed" => Time.convertDaysToSeconds(config.mean)
    case _       => distribution.sample().toInt
  }

}

package io.config

import scala.math.BigDecimal.RoundingMode

/**
  * Checks on a larva's configuration that decoding it cannot make. Each problem is a message
  * saying what to change; any one of them stops a run before it starts. The model and the web
  * interface both ask here, so they agree.
  */
object LarvaChecks {

  // probabilities meant to add up to 1 may be out by rounding, and no more
  private val Tolerance = 1e-6

  def problems(larva: LarvaConfig): Seq[String] =
    mortality(larva).toSeq ++ diel(larva) ++ ontogenetic(larva)

  // a share of the larvae, not a percentage
  private def mortality(larva: LarvaConfig): Option[String] =
    if (!larva.isMortal.getOrElse(false)) None
    else
      larva.mortalityRate match {
        case None => Some("larva.mortalityRate is missing: give the share of larvae that die each day, from 0 to 1.")
        case Some(rate) if !(rate >= 0 && rate <= 1) =>
          Some(s"larva.mortalityRate must be the share of larvae that die each day, from 0 to 1 (0.26 for 26%), not $rate.")
        case _ => None
      }

  // Diel migration always takes a larva somewhere, so its day and its night each add up to 1
  private def diel(larva: LarvaConfig): Seq[String] =
    larva.dielProbabilities.toSeq.flatMap { diel =>
      Seq("day" -> diel.day, "night" -> diel.night).flatMap { case (name, values) =>
        probabilities(s"larva.dielProbabilities.$name", values, diel.depths.size, mayAllBeZero = false)
      }
    }

  // A stage of ontogenetic migration whose probabilities are all 0 leaves a larva where it is
  private def ontogenetic(larva: LarvaConfig): Seq[String] =
    larva.ovmProbabilities.toSeq.flatMap { ovm =>
      Seq("hatching" -> ovm.hatching, "preflexion" -> ovm.preflexion, "flexion" -> ovm.flexion, "postflexion" -> ovm.postflexion)
        .flatMap { case (name, values) =>
          probabilities(s"larva.ovmProbabilities.$name", values, ovm.depths.size, mayAllBeZero = true)
        }
    }

  private def probabilities(field: String, values: List[Double], bands: Int, mayAllBeZero: Boolean): Option[String] = {
    val total = values.sum
    if (values.size != bands)
      Some(s"$field has ${values.size} probabilities for $bands depth bands: give one for each band.")
    else if (values.exists(value => !(value >= 0)))
      Some(s"$field has a probability below 0.")
    else if (math.abs(total - 1) <= Tolerance || (mayAllBeZero && total == 0)) None
    else {
      val orZero = if (mayAllBeZero) ", or be all 0 to leave larvae where they are in that stage" else ""
      Some(s"$field must add up to 1$orZero, not ${shown(total)}.")
    }
  }

  private def shown(value: Double): String =
    BigDecimal(value).setScale(6, RoundingMode.HALF_UP).bigDecimal.stripTrailingZeros.toPlainString
}

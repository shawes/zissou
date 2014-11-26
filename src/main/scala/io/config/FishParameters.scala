package io.config

import locals.VerticalMigrationPattern.VerticalMigrationPattern
import biology.{PelagicLarvalDuration, Ontogeny}
import locals.SwimmingAbility
import locals.SwimmingAbility.SwimmingAbility

class FishParameters(val name: String,
                     val savePositions: Boolean,
                     val swimmingAbility: SwimmingAbility,
                     val ontogeny: Ontogeny,
                     val swimmingSpeed: Double,
                     val verticalMigrationPattern: VerticalMigrationPattern,
                     val verticalMigrationProperties: Array[Array[Double]],
                     val pld: PelagicLarvalDuration,
                     val isMortal: Boolean,
                     val mortalityRate: Double) {
  def this() = this(null, false, null, null, 0, null, null, null, false, 0)

  def canSwim: Boolean = swimmingAbility != SwimmingAbility.Passive
}

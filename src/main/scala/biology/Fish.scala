package biology

import locals.SwimmingAbility.SwimmingAbility
import locals.VerticalMigrationPattern.VerticalMigrationPattern
import locals._

class Fish(val pld: PelagicLarvalDuration,
           val ontogeny: Ontogeny,
           val name: String,
           val savePositions: Boolean,
           val swimmingAbility: SwimmingAbility,
           val swimmingSpeed: Double,
           val verticalMigrationPattern: VerticalMigrationPattern,
           val verticalMigrationProbabilities: VerticalMigration, //Map[Int, Int], //
           val isMortal: Boolean,
           val mortalityRate: Double) {

  def this() = this(null, null, "", false, null, 0, null, null, false, 0)

  def canSwim : Boolean = swimmingAbility != SwimmingAbility.Passive
}


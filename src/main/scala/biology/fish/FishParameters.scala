package biology.fish

import locals.SwimmingAbility.SwimmingAbility
import locals._
import biology._

class FishParameters(
           val pld: PelagicLarvalDuration,
           val ontogeny: FishOntogeny,
           val name: String,
           val savePositions: Boolean,
           val swimming : Swimming,
           val verticalMigrationOntogeneticProbabilities: FishVerticalMigrationOntogenetic,
           val verticalMigrationDielProbabilities: VerticalMigrationDiel,//Map[Int, Int], //
           val isMortal: Boolean,
           val mortalityRate: Double) {

  def this() = this(null, null, "", false, null, null, null, false, 0)

  def canSwim : Boolean = swimming.ability != SwimmingAbility.Passive
}

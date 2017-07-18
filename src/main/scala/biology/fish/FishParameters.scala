package biology.fish

import biology._

class FishParameters(
           val pld: PelagicLarvalDuration,
           val ontogeny: FishOntogeny,
           val name: String,
           val savePositions: Boolean,
           val swimming : Swimming,
           val verticalMigrationOntogeneticProbabilities: FishVerticalMigrationOntogenetic,
           val verticalMigrationDielProbabilities: VerticalMigrationDiel,
           val isMortal: Boolean,
           val mortalityRate: Double) {

  def this() = this(null, null, "", false, null, null, null, false, 0)
}

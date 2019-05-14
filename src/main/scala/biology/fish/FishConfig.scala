package biology.fish

import biology._
import biology.swimming._

class FishConfig(
    val pld: PelagicLarvalDuration,
    val ontogeny: OntogenyFish,
    val name: String,
    val savePositions: Boolean,
    val swimming: Swimming,
    val ovmProbabilities: OntogeneticMigration,
    val dielProbabilities: VerticalMigrationDiel,
    val isMortal: Boolean,
    val mortalityRate: Double
) {

  def this() = this(null, null, "", false, null, null, null, false, 0)
}

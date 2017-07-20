package biology.fish

import biology.VerticalMigrationProbability
import maths.ContinuousRange

class FishVerticalMigrationOntogeneticProbability(
  depth: ContinuousRange,
  val hatching: Double,
  val preFlexion: Double,
  val flexion: Double,
  val postFlexion: Double) extends VerticalMigrationProbability(depth) {}

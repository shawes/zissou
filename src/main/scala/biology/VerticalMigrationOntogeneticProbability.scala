package biology

import maths.ContinuousRange

class VerticalMigrationOntogeneticProbability(
  depth: ContinuousRange,
  val hatching: Double,
  val preFlexion: Double,
  val flexion: Double,
  val postFlexion: Double) extends VerticalMigrationProbability(depth) {}

package biology.swimming

import maths.ContinuousRange

class VerticalMigrationOntogeneticProbability(
    depth: ContinuousRange,
    val hatching: Double,
    val preflexion: Double,
    val flexion: Double,
    val postflexion: Double
) extends VerticalMigrationProbability(depth) {}

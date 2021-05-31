package biology.swimming

import maths.ContinuousRange

trait MigrationProbability(val depth: ContinuousRange) {}

class OntogeneticMigrationProbability(
    depth: ContinuousRange,
    val hatching: Double,
    val preflexion: Double,
    val flexion: Double,
    val postflexion: Double
) extends MigrationProbability(depth) {}

class DielMigrationProbability(
    depth: ContinuousRange,
    val night: Double,
    val day: Double
) extends MigrationProbability(depth) {}

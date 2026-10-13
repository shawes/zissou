package biology.swimming

import maths.ContinuousRange

abstract class MigrationProbability(val depth: ContinuousRange) {}

class OntogeneticMigrationProbability(
    depth: ContinuousRange,
    val hatching: Double,
    val preflexion: Double,
    val flexion: Double,
    val postflexion: Double
) extends MigrationProbability(depth) {}

class DielMigrationProbability(
    depth: ContinuousRange,
    val day: Double,
    val night: Double
) extends MigrationProbability(depth) {}

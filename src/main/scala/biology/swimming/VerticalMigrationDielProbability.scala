package biology.swimming

import maths.ContinuousRange

class VerticalMigrationDielProbability(
  depth : ContinuousRange,
  val night : Double,
  val day: Double)
  extends VerticalMigrationProbability(depth) {}

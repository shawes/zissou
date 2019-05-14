package biology.swimming

import locals._

import maths.{ContinuousRange, RandomNumberGenerator}

class VerticalMigrationDiel(
    val probabilities: List[VerticalMigrationDielProbability]
) {

  def enabled: Boolean = probabilities.nonEmpty

  //TODO: Reduce complexity of this method
  def apply(dielMigration: DielVerticalMigrationType): Double = {
    var cumulativeProbability = 0.0
    val number = RandomNumberGenerator.get
    val iterator = probabilities.iterator
    var currentDepth: (ContinuousRange, Double) =
      new Tuple2(new ContinuousRange(), 0)
    val probability = iterator.next

    if (dielMigration == Day) {
      currentDepth = (probability.depth, probability.day)
    } else {
      currentDepth = (probability.depth, probability.night)
    }

    cumulativeProbability += currentDepth._2
    while (number > cumulativeProbability && iterator.hasNext) {
      val probability = iterator.next
      if (dielMigration == Day) {
        currentDepth = (probability.depth, probability.day)
      } else {
        currentDepth = (probability.depth, probability.night)
      }
      cumulativeProbability += currentDepth._2
    }
    calculateDepthInRange(currentDepth._1)
  }

  private def calculateDepthInRange(depthRange: ContinuousRange): Double = {
    RandomNumberGenerator.get(depthRange.start, depthRange.end)
  }

}

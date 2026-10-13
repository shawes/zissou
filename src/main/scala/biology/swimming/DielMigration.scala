package biology.swimming

import locals._

import maths.{ContinuousRange, RandomNumberGenerator}

class DielMigration(
    val depths: List[Double],
    val day: List[Double],
    val night: List[Double]
) {

  val probabilities: List[DielMigrationProbability] = {
    for (i <- 0 until depths.length) yield {
      if (i == 0) {
        new DielMigrationProbability(
          new ContinuousRange(0, depths(i), true),
          day(i),
          night(i)
        )
      } else {
        new DielMigrationProbability(
          new ContinuousRange(depths(i - 1) + 1, depths(i), true),
          day(i),
          night(i)
        )
      }
    }
  }.toList

  def enabled: Boolean = probabilities.nonEmpty

  /** Whether the probabilities for dawn (Day) or dusk (Night) take a larva anywhere: all 0, they leave it where it is. */
  def moves(time: DielVerticalMigrationType): Boolean =
    probabilities.exists(p => (if (time == Day) p.day else p.night) != 0)

  /** The depth a larva goes to at dawn (Day) or dusk (Night), from the depth it is at. */
  def apply(dielMigration: DielVerticalMigrationType, currentDepth: Double): Double =
    if (moves(dielMigration)) draw(dielMigration) else currentDepth

  //TODO: Reduce complexity of this method
  private def draw(dielMigration: DielVerticalMigrationType): Double = {
    var cumulativeProbability = 0.0
    val number = RandomNumberGenerator.get
    val iterator = probabilities.iterator
    var currentDepth: (ContinuousRange, Double) =
      new Tuple2(new ContinuousRange(), 0)
    val probability = iterator.next()

    if (dielMigration == Day) {
      currentDepth = (probability.depth, probability.day)
    } else {
      currentDepth = (probability.depth, probability.night)
    }

    cumulativeProbability += currentDepth._2
    while (number > cumulativeProbability && iterator.hasNext) {
      val probability = iterator.next()
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

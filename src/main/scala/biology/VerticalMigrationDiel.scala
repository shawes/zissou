package biology

import locals.DielVerticalMigrationType
import locals.DielVerticalMigrationType.DielVerticalMigrationType

import maths.{ContinuousRange, RandomNumberGenerator}

class VerticalMigrationDiel(val probabilities : List[VerticalMigrationDielProbability]) {

  def enabled : Boolean = probabilities.nonEmpty

  //TODO: Reduce complexity of this method
  def getDepth(dielMigration : DielVerticalMigrationType) : Double = {
    var cumulativeProb = 0.0
    val number = RandomNumberGenerator.get
    val iterator = probabilities.iterator
    var currentDepth: (ContinuousRange, Double) = new Tuple2(new ContinuousRange(), 0)
    val prob = iterator.next

    if(dielMigration == DielVerticalMigrationType.Day) {
      currentDepth = (prob.depth, prob.day)
    } else {
      currentDepth = (prob.depth, prob.night)
    }

    cumulativeProb += currentDepth._2
    while (number > cumulativeProb && iterator.hasNext) {
      val prob = iterator.next
          if(dielMigration == DielVerticalMigrationType.Day) {
            currentDepth = (prob.depth, prob.day)
          } else {
            currentDepth = (prob.depth, prob.night)
          }
      cumulativeProb += currentDepth._2
    }
    calculateDepthInRange(currentDepth._1)
  }

  private def calculateDepthInRange(depthRange: ContinuousRange): Double = {
    RandomNumberGenerator.get(depthRange.start, depthRange.end)
  }


}

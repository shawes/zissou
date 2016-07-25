package biology

import locals.OntogenyState
import locals.OntogenyState.OntogenyState
import maths.{ContinuousRange, RandomNumberGenerator}

import scala.collection.mutable.ListBuffer

class VerticalMigration(val probabilities: List[VerticalMigrationProbability]) {

  def getDepth(ontogeny: OntogenyState): Double = {
    val list: ListBuffer[(ContinuousRange, Double)] = ListBuffer.empty[(ContinuousRange, Double)]
    probabilities.foreach(vmp => {
      list append getProbability(vmp, ontogeny)
    })

    var cumulativeProb = 0.0
    val number = RandomNumberGenerator.get

    val iterator = list.iterator
    var currentDepth: (ContinuousRange, Double) = new Tuple2(new ContinuousRange(), 0)
    currentDepth = iterator.next
    cumulativeProb += currentDepth._2
    while (number > cumulativeProb && iterator.hasNext) {
      currentDepth = iterator.next
      cumulativeProb += currentDepth._2
    }

    calculateDepthInRange(currentDepth._1)
  }

  private def getProbability(prob: VerticalMigrationProbability, ontogeny: OntogenyState): (ContinuousRange, Double) = ontogeny match {
    case OntogenyState.Hatching => new Tuple2(prob.depth, prob.hatching)
    case OntogenyState.Preflexion => new Tuple2(prob.depth, prob.preFlexion)
    case OntogenyState.Flexion => new Tuple2(prob.depth, prob.flexion)
    case _ => new Tuple2(prob.depth, prob.postFlexion)
  }

  private def calculateDepthInRange(depthRange: ContinuousRange): Double = {
    RandomNumberGenerator.get(depthRange.start, depthRange.end)
  }
}

package biology.swimming

import locals._
import maths.{ContinuousRange, RandomNumberGenerator}
import scala.collection.mutable.ListBuffer

class VerticalMigrationOntogenetic(
    val implementation: OntogeneticVerticalMigrationImpl,
    val probabilities: List[VerticalMigrationOntogeneticProbability]
) {

  def enabled: Boolean = probabilities.nonEmpty

  def getDepth(ontogeny: OntogeneticState, currentDepth: Double): Double = {
    implementation match {
      case TimeStepMigration => getDepthRestricted(ontogeny, currentDepth)
      case _                 => getDepthRandom(ontogeny)
    }
  }

  private def getDepthRandom(ontogeny: OntogeneticState): Double = {
    val list: ListBuffer[(ContinuousRange, Double)] =
      ListBuffer.empty[(ContinuousRange, Double)]
    probabilities.foreach(vmp => {
      list append getProbability(vmp, ontogeny)
    })

    var cumulativeProb = 0.0
    val number = RandomNumberGenerator.get

    val iterator = list.iterator
    var currentDepth: (ContinuousRange, Double) =
      new Tuple2(new ContinuousRange(), 0)
    currentDepth = iterator.next
    cumulativeProb += currentDepth._2
    while (number > cumulativeProb && iterator.hasNext) {
      currentDepth = iterator.next
      cumulativeProb += currentDepth._2
    }
    calculateDepthInRange(currentDepth._1)
  }

  //TODO: Remove the complexity from this method
  private def getDepthRestricted(
      ontogeny: OntogeneticState,
      depth: Double
  ): Double = {
    val list: ListBuffer[(ContinuousRange, Double)] =
      ListBuffer.empty[(ContinuousRange, Double)]
    probabilities.foreach(vmp => {
      list append getProbability(vmp, ontogeny)
    })

    val depthBin = list.find(x => x._1.contains(depth))
    val depthIndex = list.indexOf(depthBin)
    val number = RandomNumberGenerator.get

    if (depthBin != None && depthIndex > 0 && depthIndex < list.size - 1) {
      val upperDepthBin = list(depthIndex - 1)
      val lowerDepthBin = list(depthIndex + 1)
      val probabilitiesTotal = lowerDepthBin._2 + depthBin.get._2 + upperDepthBin._2
      val newProbabilities = Tuple3(
        upperDepthBin._2 / probabilitiesTotal,
        depthBin.get._2 / probabilitiesTotal,
        lowerDepthBin._2 / probabilitiesTotal
      )
      if (number < newProbabilities._1) {
        return calculateDepthInRange(upperDepthBin._1)
      } else if (number < (newProbabilities._1 + newProbabilities._2)) {
        return calculateDepthInRange(depthBin.get._1)
      } else {
        return calculateDepthInRange(lowerDepthBin._1)
      }
    } else if (depthBin != None && depthIndex == 0 && depthIndex < list.size - 1) {
      val lowerDepthBin = list(depthIndex + 1)
      val probabilitiesTotal = lowerDepthBin._2 + depthBin.get._2
      val newProbabilities = Tuple2(
        depthBin.get._2 / probabilitiesTotal,
        lowerDepthBin._2 / probabilitiesTotal
      )
      if (number < newProbabilities._1) {
        return calculateDepthInRange(depthBin.get._1)
      } else {
        return calculateDepthInRange(lowerDepthBin._1)
      }
    } else if (depthBin != None && depthIndex > 0 && depthIndex == list.size - 1) {
      val upperDepthBin = list(depthIndex - 1)
      val probabilitiesTotal = upperDepthBin._2 + depthBin.get._2
      val newProbabilities = Tuple2(
        upperDepthBin._2 / probabilitiesTotal,
        depthBin.get._2 / probabilitiesTotal
      )
      if (number < newProbabilities._1) {
        return calculateDepthInRange(upperDepthBin._1)
      } else {
        return calculateDepthInRange(depthBin.get._1)
      }
    }
    return depth
  }

  private def calculateDepthInRange(depthRange: ContinuousRange): Double = {
    RandomNumberGenerator.get(depthRange.start, depthRange.end)
  }

  private def getProbability(
      prob: VerticalMigrationOntogeneticProbability,
      ontogeny: OntogeneticState
  ): (ContinuousRange, Double) =
    ontogeny match {
      case Hatching => new Tuple2(prob.depth, prob.hatching)
      case Preflexion =>
        new Tuple2(prob.depth, prob.preflexion)
      case Flexion => new Tuple2(prob.depth, prob.flexion)
      case _       => new Tuple2(prob.depth, prob.postflexion)
    }
}

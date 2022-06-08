package biology.swimming

import locals.Enums._
import locals.Enums.OntogeneticState._
import io.config._
import maths.{ContinuousRange, RandomNumberGenerator}
import scala.collection.mutable.ListBuffer

class OntogeneticMigration(
    val config: OntogeneticMigrationConfig
) {

  val implementation = config.implementation match {
    case "stage"    => OntogeneticMigrationType.Stage
    case "daily"    => OntogeneticMigrationType.Daily
    case "timestep" => OntogeneticMigrationType.TimeStep
    case _          => OntogeneticMigrationType.Stage
  }

  val probabilities: List[OntogeneticMigrationProbability] = {
    for (i <- 0 until config.depths.length) yield {
      if (i == 0) then {
        new OntogeneticMigrationProbability(
          new ContinuousRange(0, config.depths(i), true),
          config.hatching(i),
          config.preflexion(i),
          config.flexion(i),
          config.postflexion(i)
        )
      } else {
        new OntogeneticMigrationProbability(
          new ContinuousRange(config.depths(i - 1) + 1, config.depths(i), true),
          config.hatching(i),
          config.preflexion(i),
          config.flexion(i),
          config.postflexion(i)
        )
      }
    }
  }.toList

  def enabled: Boolean = probabilities.nonEmpty

  def apply(ontogeny: OntogeneticState, currentDepth: Double): Double = {
    implementation match {
      case OntogeneticMigrationType.TimeStep =>
        getDepthRestricted(ontogeny, currentDepth)
      case _ => getDepthRandom(ontogeny)
    }
  }

  private def getDepthRandom(ontogeny: OntogeneticState): Double = {
    val list: ListBuffer[(ContinuousRange, Double)] =
      ListBuffer.empty[(ContinuousRange, Double)]
    probabilities.foreach(vmp => {
      list append getProbability(vmp, ontogeny)
    })

    var cumulativeProbability = 0.0
    val number = RandomNumberGenerator.get

    val iterator = list.iterator
    var currentDepth: (ContinuousRange, Double) =
      new Tuple2(new ContinuousRange(), 0)
    currentDepth = iterator.next()
    cumulativeProbability += currentDepth._2
    while (number > cumulativeProbability && iterator.hasNext) do {
      currentDepth = iterator.next()
      cumulativeProbability += currentDepth._2
    }
    calculateDepthInRange(currentDepth._1)
  }

  // TODO: Remove the complexity from this method
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

    if (depthBin != None && depthIndex > 0 && depthIndex < list.size - 1) then {
      val upperDepthBin = list(depthIndex - 1)
      val lowerDepthBin = list(depthIndex + 1)
      val probabilitiesTotal =
        lowerDepthBin._2 + depthBin.get._2 + upperDepthBin._2
      val newProbabilities = Tuple3(
        upperDepthBin._2 / probabilitiesTotal,
        depthBin.get._2 / probabilitiesTotal,
        lowerDepthBin._2 / probabilitiesTotal
      )
      if (number < newProbabilities._1) then {
        return calculateDepthInRange(upperDepthBin._1)
      } else if (number < (newProbabilities._1 + newProbabilities._2)) then {
        return calculateDepthInRange(depthBin.get._1)
      } else {
        return calculateDepthInRange(lowerDepthBin._1)
      }
    } else if (
      depthBin != None && depthIndex == 0 && depthIndex < list.size - 1
    ) then {
      val lowerDepthBin = list(depthIndex + 1)
      val probabilitiesTotal = lowerDepthBin._2 + depthBin.get._2
      val newProbabilities = Tuple2(
        depthBin.get._2 / probabilitiesTotal,
        lowerDepthBin._2 / probabilitiesTotal
      )
      if (number < newProbabilities._1) then {
        return calculateDepthInRange(depthBin.get._1)
      } else {
        return calculateDepthInRange(lowerDepthBin._1)
      }
    } else if (
      depthBin != None && depthIndex > 0 && depthIndex == list.size - 1
    ) then {
      val upperDepthBin = list(depthIndex - 1)
      val probabilitiesTotal = upperDepthBin._2 + depthBin.get._2
      val newProbabilities = Tuple2(
        upperDepthBin._2 / probabilitiesTotal,
        depthBin.get._2 / probabilitiesTotal
      )
      if (number < newProbabilities._1) then {
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
      probability: OntogeneticMigrationProbability,
      ontogeny: OntogeneticState
  ): (ContinuousRange, Double) =
    ontogeny match {
      case Hatching => new Tuple2(probability.depth, probability.hatching)
      case Preflexion =>
        new Tuple2(probability.depth, probability.preflexion)
      case Flexion => new Tuple2(probability.depth, probability.flexion)
      case _       => new Tuple2(probability.depth, probability.postflexion)
    }
}

case class OntogeneticMigrationVariables(
    recentlyDeveloped: Boolean = false,
    isMidnight: Boolean = false
)

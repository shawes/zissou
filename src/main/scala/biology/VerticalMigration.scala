package biology

import biology.fish.{FishVerticalMigrationOntogenetic,FishVerticalMigrationOntogeneticProbability}
import locals.OntogenyState
import locals.OntogenyState.OntogenyState
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._

class VerticalMigration(val ontogeneticProbabilities: List[FishVerticalMigrationOntogeneticProbability], val dielProbabilities: List[VerticalMigrationDielProbability]) {

  val ontogeneticMigration = new FishVerticalMigrationOntogenetic(ontogeneticProbabilities)
  val dielMigration = new VerticalMigrationDiel(dielProbabilities)

  def getOntogeneticDepth(ontogeny: OntogenyState): Double = {
    ontogeneticMigration.getDepth(ontogeny)
  }

  def getDielDepth(location: GeoCoordinate, currentTime : DateTime, timeZone : DateTimeZone, timeStep : Double): Double = {
    dielMigration.getDepth(location, currentTime, timeZone, timeStep)
  }

}

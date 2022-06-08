package biology

import locals.Enums.OntogeneticState
import locals.Enums.OntogeneticState._

//import OntogeneticState.*

trait Ontogeny {
  var ontogeny: OntogeneticState = getOntogeneticStateForAge(0)
  var recentlyDeveloped: Boolean = false
  def getOntogeneticStateForAge(age: Int): OntogeneticState
}

trait OntogenyFish extends Ontogeny {
  val hatching: Int
  val preflexion: Int
  val flexion: Int
  val postflexion: Int
  override def getOntogeneticStateForAge(age: Int): OntogeneticState = {
    if (age < preflexion) then Hatching
    else if (age >= preflexion && age < flexion) then Preflexion
    else if (age >= flexion && age < postflexion) then Flexion
    else Postflexion
  }
}

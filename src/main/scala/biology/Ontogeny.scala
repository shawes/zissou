package biology

import locals._

trait Ontogeny {
  val hatching: Int
  def getState(age: Int): OntogeneticState
}

trait OntogenyFish extends Ontogeny {
  val preflexion: Int
  val flexion: Int
  val postflexion: Int
  def getState(age: Int): OntogeneticState = {
    if (age < preflexion) Hatching
    else if (age >= preflexion && age < flexion) Preflexion
    else if (age >= flexion && age < postflexion) Flexion
    else Postflexion
  }
}

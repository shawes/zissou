package biology

import locals._

trait Ontogeny {
  var hatching: Int
  def getState(age: Int): OntogeneticState
}

trait OntogenyFish extends Ontogeny {
  var preflexion: Int
  var flexion: Int
  var postflexion: Int
  def getState(age: Int): OntogeneticState = {
    if (age < preflexion) Hatching
    else if (age >= preflexion && age < flexion) Preflexion
    else if (age >= flexion && age < postflexion) Flexion
    else Postflexion
  }
}

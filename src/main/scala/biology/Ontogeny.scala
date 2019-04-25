package biology

import locals.OntogenyState
import locals.OntogenyState.OntogenyState

trait Ontogeny {
  var hatching : Int
  def getState(age: Int): OntogenyState
}

trait OntogenyFish extends Ontogeny {
  var preflexion : Int
  var flexion : Int
  var postflexion : Int
  def getState(age: Int): OntogenyState = {
    if (age < preflexion) OntogenyState.Hatching
    else if (age >= preflexion && age < flexion) OntogenyState.Preflexion
    else if (age >= flexion && age < postflexion) OntogenyState.Flexion
    else OntogenyState.Postflexion
  }
}
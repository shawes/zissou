package biology.fish

import locals.OntogenyState
import locals.OntogenyState.OntogenyState
import biology.Ontogeny
import utilities.Time

class FishOntogeny(val preflexion: Int, val flexion: Int, val postflexion: Int) extends Ontogeny {
  def getState(age: Int): OntogenyState = {
    if (age < preflexion) OntogenyState.Hatching
    else if (age >= preflexion && age < flexion) OntogenyState.Preflexion
    else if (age >= flexion && age < postflexion) OntogenyState.Flexion
    else OntogenyState.Postflexion
  }
}

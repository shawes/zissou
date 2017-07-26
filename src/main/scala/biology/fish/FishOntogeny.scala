package biology.fish

import locals.OntogenyState
import locals.OntogenyState.OntogenyState
import biology.Ontogeny
import maths.Time

class FishOntogeny(val preFlexion: Int, val flexion: Int, val postFlexion: Int) extends Ontogeny {

  //def preFlexion() : Int = Time.convertDaysToSeconds(preFlexion)
  def getState(age: Int): OntogenyState = {

    var state: OntogenyState = OntogenyState.Postflexion

    if (age < preFlexion) {
      state = OntogenyState.Hatching
    } else if (age >= preFlexion && age < flexion) {
      state = OntogenyState.Preflexion
    } else if (age >= flexion && age < postFlexion) {
      state = OntogenyState.Flexion
    }
    state
  }
}

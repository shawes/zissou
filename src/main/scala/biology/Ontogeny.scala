package biology

import locals.OntogenyState
import locals.OntogenyState.OntogenyState

class Ontogeny(val preFlexion: Int, val flexion: Int, val postFlexion: Int) {

  def getState(age: Double): OntogenyState = {

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

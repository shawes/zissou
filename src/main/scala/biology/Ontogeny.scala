package biology

import locals.OntogenyState
import locals.OntogenyState.OntogenyState

class Ontogeny(val preFlexion: Double, val flexion: Double, val postFlexion: Double) {

  def getState(age: Double): OntogenyState = {

    var state: OntogenyState = OntogenyState.Postflexion

    if (age < preFlexion) {
      state = OntogenyState.Hatching
    } else if (age < flexion) {
      state = OntogenyState.Preflexion
    } else if (age < postFlexion) {
      state = OntogenyState.Flexion
    }
    state
  }
}

package biology

import locals.OntogenyState.OntogenyState

abstract class Ontogeny {

  def getState(age: Int): OntogenyState

}

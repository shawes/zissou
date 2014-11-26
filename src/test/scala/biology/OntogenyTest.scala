package biology

import org.scalatest.FlatSpec

class OntogenyTest extends FlatSpec {
  val ontogeny = new Ontogeny(1, 2, 3)

  "An ontogeny object" should "not be null upon construction" in {
    assert(ontogeny != null)
  }

  it should "have the correct pre-flexion" in {
    assert(ontogeny.preFlexion == 1)
  }

  it should "have the correct flexion" in {
    assert(ontogeny.flexion == 2)
  }

  it should "have the correct post-flexion" in {
    assert(ontogeny.postFlexion == 3)
  }
}

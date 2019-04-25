package biology.fish

import locals._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class FishOntogenyTest extends FlatSpec {
  val preflexion = 1000
  val flexion = 2000
  val postflexion = 3000

  val ontogeny = new FishOntogeny(preflexion, flexion, postflexion)

  "An ontogeny object" should "not be null upon construction" in {
    assert(ontogeny != null)
  }

  it should "have the correct pre-flexion" in {
    assert(ontogeny.preflexion == 1000)
  }

  it should "have the correct flexion" in {
    assert(ontogeny.flexion == 2000)
  }

  it should "have the correct post-flexion" in {
    assert(ontogeny.postflexion == 3000)
  }

  it should "give the ontogeny state as hatching" in {
    val result = ontogeny.getState(500)
    result should equal(Hatching)
  }

  it should "give the ontogeny state as preflexion" in {
    val result = ontogeny.getState(1500)
    result should equal(Preflexion)
  }

  it should "give the ontogeny state as preflexion if equal" in {
    val result = ontogeny.getState(1000)
    result should equal(Preflexion)
  }

  it should "give the ontogeny state as flexion" in {
    val result = ontogeny.getState(2500)
    result should equal(Flexion)
  }

  it should "give the ontogeny state as flexion if equal" in {
    val result = ontogeny.getState(2000)
    result should equal(Flexion)
  }

  it should "give the ontogeny state as postflexion" in {
    val result = ontogeny.getState(3500)
    result should equal(Postflexion)
  }

  it should "give the ontogeny state as postflexion if equal" in {
    val result = ontogeny.getState(3000)
    result should equal(Postflexion)
  }

}

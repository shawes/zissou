package biology

import locals.Enums.OntogeneticState._
import org.scalatest._
import matchers.should._

class FishOntogenyTest extends flatspec.AnyFlatSpec with Matchers {
  val hatching = 0
  val preflexion = 1000
  val flexion = 2000
  val postflexion = 3000

  class OntogenyTest(
      override val hatching: Int,
      override val preflexion: Int,
      override val flexion: Int,
      override val postflexion: Int
  ) extends OntogenyFish {}

  val ontogeny = new OntogenyTest(hatching, preflexion, flexion, postflexion)

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
    val result = ontogeny.getOntogeneticStateForAge(500)
    result should equal(Hatching)
  }

  it should "give the ontogeny state as preflexion" in {
    val result = ontogeny.getOntogeneticStateForAge(1500)
    result should equal(Preflexion)
  }

  it should "give the ontogeny state as preflexion if equal" in {
    val result = ontogeny.getOntogeneticStateForAge(1000)
    result should equal(Preflexion)
  }

  it should "give the ontogeny state as flexion" in {
    val result = ontogeny.getOntogeneticStateForAge(2500)
    result should equal(Flexion)
  }

  it should "give the ontogeny state as flexion if equal" in {
    val result = ontogeny.getOntogeneticStateForAge(2000)
    result should equal(Flexion)
  }

  it should "give the ontogeny state as postflexion" in {
    val result = ontogeny.getOntogeneticStateForAge(3500)
    result should equal(Postflexion)
  }

  it should "give the ontogeny state as postflexion if equal" in {
    val result = ontogeny.getOntogeneticStateForAge(3000)
    result should equal(Postflexion)
  }

}

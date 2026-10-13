package model

import org.scalatest.flatspec.AnyFlatSpec
import tools.ModelFixture
import utilities.Time

class OldAgeTest extends AnyFlatSpec {

  private val inputs = ModelFixture.inputs()

  "A larva over a reef" should "settle as soon as it is competent" in {
    val model = ModelFixture.run(ModelFixture.yaml(inputs, ModelFixture.driftingLarva(pld = 3, nonSettlementPeriod = 1)))
    val larvae = model.biology.stationaryLarvae
    assert(larvae.size == 50)
    assert(larvae.forall(_.isSettled))
    // competent once older than a day, which is the first 2 hour step after it
    assert(larvae.forall(larva => Time.convertSecondsToDays(larva.age) == 1 + 2.0 / 24))
  }

  "A larva that reaches the end of its pelagic larval duration" should "die, not settle on the step it dies" in {
    // competent only once older than its PLD, which is when it dies of old age
    val model = ModelFixture.run(ModelFixture.yaml(inputs, ModelFixture.driftingLarva(pld = 3, nonSettlementPeriod = 3)))
    val larvae = model.biology.stationaryLarvae
    assert(larvae.size == 50)
    assert(larvae.count(_.isSettled) == 0)
    assert(larvae.forall(_.isDead))
  }

  "No larva" should "settle older than its pelagic larval duration" in {
    val model = ModelFixture.run(ModelFixture.yaml(inputs, ModelFixture.driftingLarva(pld = 3, nonSettlementPeriod = 2.95)))
    val settled = model.biology.stationaryLarvae.filter(_.isSettled)
    assert(settled.nonEmpty)
    assert(settled.forall(larva => larva.age <= larva.pelagicLarvalDuration))
  }
}

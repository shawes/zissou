package model

import org.scalatest.flatspec.AnyFlatSpec
import tools.ModelFixture

class TemperatureMortalityModelTest extends AnyFlatSpec {

  // 50% of the larvae die in a day at 20 degrees, with the hazard doubling for every 10 degrees warmer
  private def larva(extra: String = "") = ModelFixture.driftingLarva(
    pld = 3, nonSettlementPeriod = 3, mortalityRate = Some(0.5),
    extra = """  temperatureMortality:
              |    referenceTemperature: 20
              |    q10: 2
              |""".stripMargin + extra)

  // the larvae that live out their three days and die of old age: all but those that died of what the sea did to them
  private def survivors(temperature: Double, larvae: Int = 2000) = {
    val inputs = ModelFixture.inputs(temperature = Some(temperature))
    val model = ModelFixture.run(ModelFixture.yaml(inputs, larva(), numberOfLarvae = larvae))
    val all = model.biology.stationaryLarvae
    assert(all.size == larvae && all.forall(_.isDead))
    (all.count(l => l.age > l.pelagicLarvalDuration), model)
  }

  "Larvae that die by temperature" should "die at the configured rate at the reference temperature" in {
    val (lived, model) = survivors(20.0)
    // an eighth live to the end of three days, 250 of 2,000: a standard deviation of 15
    assert(lived > 190 && lived < 310, s"$lived of 2000 lived")
    val Some((mean, checks, unknown)) = model.biology.temperatureMortalitySummary
    assert(math.abs(mean - 0.5) < 1e-9 && checks > 2000 && unknown == 0)
  }

  it should "die faster in warm water: with a Q10 of 2, 10 degrees warmer takes 50% a day to 75%" in {
    val (lived, model) = survivors(30.0)
    // 25% survive each day: 1.6% over three days, 31 of 2,000
    assert(lived < 70, s"$lived of 2000 lived")
    assert(math.abs(model.biology.temperatureMortalitySummary.get._1 - 0.75) < 1e-9)
  }

  it should "die more slowly in cool water: 10 degrees cooler takes 50% a day to 29%" in {
    val (lived, model) = survivors(10.0)
    // 70.7% survive each day: 35% over three days, 707 of 2,000, a standard deviation of 21
    assert(lived > 640 && lived < 775, s"$lived of 2000 lived")
    assert(math.abs(model.biology.temperatureMortalitySummary.get._1 - (1 - math.sqrt(0.5))) < 1e-9)
  }

  it should "be told apart from a run that does not ask for it, which is as it always was" in {
    val inputs = ModelFixture.inputs() // no temperature files at all
    val plain = ModelFixture.driftingLarva(pld = 3, nonSettlementPeriod = 3, mortalityRate = Some(0.5))
    val model = ModelFixture.run(ModelFixture.yaml(inputs, plain, numberOfLarvae = 2000))
    assert(model.biology.temperatureMortalitySummary.isEmpty)
    val lived = model.biology.stationaryLarvae.count(l => l.age > l.pelagicLarvalDuration)
    assert(lived > 190 && lived < 310, s"$lived of 2000 lived")
  }

  it should "stop the run before it starts, saying why, when there are no temperature files" in {
    val inputs = ModelFixture.inputs() // none
    val problem = intercept[IllegalArgumentException](ModelFixture.run(ModelFixture.yaml(inputs, larva(), numberOfLarvae = 10)))
    assert(problem.getMessage.contains("temp flow file"), problem.getMessage)
  }
}

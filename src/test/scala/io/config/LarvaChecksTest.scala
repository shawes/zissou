package io.config

import java.io.File
import org.scalatest.flatspec.AnyFlatSpec
import tools.ModelFixture

class LarvaChecksTest extends AnyFlatSpec {

  private def problems(diel: String = Diel, ovm: String = Ovm): Seq[String] = {
    val larva = ModelFixture.driftingLarva(extra = diel + ovm)
    LarvaChecks.problems(ModelFixture.configuration(ModelFixture.yaml(new File("unused"), larva)).larva)
  }

  // the synthetic configs' migration: 0.35 + 0.5 + 0.15 is 1 only to within rounding, and the
  // hatching stage is all 0
  private val Diel =
    """  dielProbabilities:
      |    depths: [25, 50, 75, 100]
      |    day: [0.1, 0.3, 0.4, 0.2]
      |    night: [0.3, 0.3, 0.3, 0.1]
      |""".stripMargin
  private val Ovm =
    """  ovmProbabilities:
      |    implementation: stage
      |    depths: [5, 50, 100]
      |    hatching: [0, 0, 0]
      |    preflexion: [0.4, 0.5, 0.1]
      |    flexion: [0.35, 0.5, 0.15]
      |    postflexion: [0.05, 0.85, 0.1]
      |""".stripMargin

  "Migration probabilities" should "be accepted when each set adds up to 1, or is all 0" in {
    assert(problems().isEmpty)
    assert(problems(diel = "", ovm = "").isEmpty, "no migration at all")
    assert(problems(diel = Diel.replace("night: [0.3, 0.3, 0.3, 0.1]", "night: [0, 0, 0, 0]")).isEmpty, "a diel night of all 0")
    assert(problems(diel = Diel.replace("day: [0.1, 0.3, 0.4, 0.2]", "day: [0, 0, 0, 0]")).isEmpty, "a diel day of all 0")
  }

  it should "add up to 1 for diel migration, by day and by night" in {
    assert(problems(diel = Diel.replace("day: [0.1, 0.3, 0.4, 0.2]", "day: [0.1, 0.3, 0.4, 0.1]")) ==
      Seq("larva.dielProbabilities.day must add up to 1, or be all 0 to leave larvae where they are at dawn, not 0.9."))
    assert(problems(diel = Diel.replace("night: [0.3, 0.3, 0.3, 0.1]", "night: [0.3, 0.3, 0.3, 0.2]")) ==
      Seq("larva.dielProbabilities.night must add up to 1, or be all 0 to leave larvae where they are at dusk, not 1.1."))
  }

  it should "add up to 1, or be all 0, for each stage of ontogenetic migration" in {
    assert(problems(ovm = Ovm.replace("flexion: [0.35, 0.5, 0.15]", "flexion: [0.35, 0.5, 0.25]")) ==
      Seq("larva.ovmProbabilities.flexion must add up to 1, or be all 0 to leave larvae where they are in that stage, not 1.1."))
  }

  it should "not be percentages" in {
    assert(problems(diel = Diel.replace("day: [0.1, 0.3, 0.4, 0.2]", "day: [10, 30, 40, 20]")) ==
      Seq("larva.dielProbabilities.day must add up to 1, or be all 0 to leave larvae where they are at dawn, not 100."))
  }

  it should "give one probability for each depth band" in {
    assert(problems(diel = Diel.replace("night: [0.3, 0.3, 0.3, 0.1]", "night: [0.3, 0.3, 0.4]")) ==
      Seq("larva.dielProbabilities.night has 3 probabilities for 4 depth bands: give one for each band."))
    assert(problems(ovm = Ovm.replace("hatching: [0, 0, 0]", "hatching: [0, 0, 0, 0]")) ==
      Seq("larva.ovmProbabilities.hatching has 4 probabilities for 3 depth bands: give one for each band."))
  }

  it should "not be below 0" in {
    assert(problems(ovm = Ovm.replace("postflexion: [0.05, 0.85, 0.1]", "postflexion: [-0.05, 0.95, 0.1]")) ==
      Seq("larva.ovmProbabilities.postflexion has a probability below 0."))
  }

  it should "stop the model before it starts, with what to change" in {
    val inputs = ModelFixture.inputs()
    val larva = ModelFixture.driftingLarva(extra = Diel.replace("night: [0.3, 0.3, 0.3, 0.1]", "night: [0.3, 0.3, 0.4]") + Ovm)
    val message = intercept[IllegalArgumentException](ModelFixture.run(ModelFixture.yaml(inputs, larva))).getMessage
    assert(message == "larva.dielProbabilities.night has 3 probabilities for 4 depth bands: give one for each band.")
  }
}

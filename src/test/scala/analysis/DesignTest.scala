package analysis

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec

class DesignTest extends AnyFlatSpec {

  private def design(yaml: String) = io.circe.yaml.parser.parse(yaml).toOption.flatMap(j => Some(Design.parse(j))).get

  private val minimal =
    """baseline: base.yaml
      |output: out
      |""".stripMargin

  "A design" should "list its named runs and a run for each value of each setting varied one at a time" in {
    val d = design(minimal +
      """parallel: 3
        |runs:
        |  - name: baseline
        |  - name: few
        |    set:
        |      spawn.spawningLocation[*].numberOfLarvae: 300
        |oneAtATime:
        |  larva.pelagicLarvalDuration.nonSettlementPeriod: [5, 15]
        |  habitat.buffer.settlement: [1.5]
        |""".stripMargin).toOption.get
    assert(d.parallel == 3)
    assert(d.runs.map(_.name) == Seq("baseline", "few", "nonSettlementPeriod-5", "nonSettlementPeriod-15", "settlement-1.5"))
    assert(d.runs(1).settings == Seq("spawn.spawningLocation[*].numberOfLarvae" -> Json.fromInt(300)))
    assert(d.runs(2).label == "larva.pelagicLarvalDuration.nonSettlementPeriod = 5")
  }

  it should "run every run once with each seed, named for it" in {
    val d = design(minimal +
      """seeds: [1, 2]
        |runs:
        |  - name: baseline
        |  - name: other
        |""".stripMargin).toOption.get
    assert(d.runs.map(_.name) == Seq("baseline-s1", "baseline-s2", "other-s1", "other-s2"))
    assert(d.runs.head.settings == Seq("settings.randomSeed" -> Json.fromInt(1)) && d.runs.head.seed.contains(1))
    assert(d.runs.head.label == "baseline")
  }

  it should "read the regions to count settlers in" in {
    val d = design(minimal + "regions: {victoria: [1000000, 1999999]}\nruns: [{name: a}]\n").toOption.get
    assert(d.regions == Seq(("victoria", 1000000L, 1999999L)))
  }

  it should "say what is wrong with it" in {
    assert(design("output: out\nruns: [{name: a}]\n") == Left("The design needs a 'baseline' configuration file."))
    assert(design(minimal) == Left("The design has no runs: give 'runs' or 'oneAtATime'."))
    assert(design(minimal + "runs: [{name: a}, {name: a}]\n") == Left("Two runs are named a."))
    assert(design(minimal + "runs: [{name: 'a b'}]\n").left.toOption.get.contains("may only have"))
    assert(design(minimal + "oneAtATime: {larva.x: []}\n").left.toOption.get.contains("must be a list of one or more"))
    assert(design(minimal + "runs: [{name: a, set: {'larva..x': 1}}]\n").left.toOption.get.contains("is not a path"))
    assert(design(minimal + "parallel: 0\nruns: [{name: a}]\n") == Left("'parallel' must be at least 1."))
  }

  private val config = io.circe.yaml.parser.parse(
    """larva:
      |  mortalityRate: 0.1
      |spawn:
      |  spawningLocation:
      |    - {name: a, numberOfLarvae: 10}
      |    - {name: b, numberOfLarvae: 20}
      |""".stripMargin).toOption.get

  "Setting a path" should "change the setting there, in one item of a list or all of them" in {
    val one = Paths.set(config, "spawn.spawningLocation[1].numberOfLarvae", Json.fromInt(5)).toOption.get
    assert(one.hcursor.downField("spawn").downField("spawningLocation").downN(0).get[Int]("numberOfLarvae").contains(10))
    assert(one.hcursor.downField("spawn").downField("spawningLocation").downN(1).get[Int]("numberOfLarvae").contains(5))
    val all = Paths.set(config, "spawn.spawningLocation[*].numberOfLarvae", Json.fromInt(7)).toOption.get
    assert(all.hcursor.downField("spawn").downField("spawningLocation").values.get.forall(_.hcursor.get[Int]("numberOfLarvae").contains(7)))
    assert(Paths.set(config, "larva.mortalityRate", Json.fromDoubleOrNull(0.2)).toOption.get
      .hcursor.downField("larva").get[Double]("mortalityRate").contains(0.2))
  }

  it should "add a setting the configuration leaves out, but not a group it does not have" in {
    val added = Paths.set(config, "larva.temperatureMortality", Json.obj("q10" -> Json.fromInt(2))).toOption.get
    assert(added.hcursor.downField("larva").downField("temperatureMortality").get[Int]("q10").contains(2))
    assert(Paths.set(config, "larva.swimming.endurance", Json.fromInt(1)).left.toOption.get.contains("has no swimming"))
    assert(Paths.set(config, "spawn.spawningLocation[2].numberOfLarvae", Json.fromInt(1)).left.toOption.get.contains("only 2 items"))
    assert(Paths.set(config, "larva[0].mortalityRate", Json.fromInt(1)).left.toOption.get.contains("not a list"))
  }
}

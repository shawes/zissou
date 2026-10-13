package ui

import java.nio.file.{Files, Path}
import org.scalatest.flatspec.AnyFlatSpec

class ConfigStoreTest extends AnyFlatSpec {

  private def store(): (ConfigStore, Path) = {
    val base = Files.createTempDirectory("zissou-config-test")
    (new ConfigStore(base.resolve("configs"), base), base)
  }

  "A configuration name" should "be a plain file name ending in .yaml or .yml" in {
    val (configs, _) = store()
    assert(Seq("smoke.yaml", "run-1.yml", "a_b.2.yaml").forall(configs.isValidName))
  }

  it should "never be able to leave the folder" in {
    val (configs, _) = store()
    val bad = Seq("../x.yaml", "a/b.yaml", "..\\x.yaml", "/etc/passwd.yaml", ".hidden.yaml", "x.txt", "x", "", "x.yaml/", "a%2Fb.yaml", "x.yaml\u0000.txt")
    assert(bad.forall(n => !configs.isValidName(n)), bad.filter(configs.isValidName).toString)
  }

  "The store" should "list, read and write configuration files" in {
    val (configs, _) = store()
    assert(configs.list().isEmpty)
    configs.write("b.yaml", "two")
    configs.write("a.yaml", "one")
    assert(configs.list().map(_.name) == Seq("a.yaml", "b.yaml"))
    assert(configs.read("a.yaml").contains("one"))
    configs.write("a.yaml", "changed")
    assert(configs.read("a.yaml").contains("changed"))
    assert(configs.read("missing.yaml").isEmpty)
  }

  it should "refuse to write a name that is not valid, and leave no stray files" in {
    val (configs, base) = store()
    assertThrows[IllegalArgumentException](configs.write("../escape.yaml", "x"))
    assert(!Files.exists(base.resolve("escape.yaml")))
    configs.write("ok.yaml", "x")
    val files = Files.list(base.resolve("configs")).toArray.map(_.toString)
    assert(files.length == 1, files.toList.toString)
  }

  it should "ignore files that are not configurations" in {
    val (configs, base) = store()
    Files.createDirectories(base.resolve("configs"))
    Files.writeString(base.resolve("configs").resolve("notes.txt"), "x")
    Files.createDirectories(base.resolve("configs").resolve("folder.yaml"))
    configs.write("real.yaml", "x")
    assert(configs.list().map(_.name) == Seq("real.yaml"))
  }

  "Validation" should "accept a complete configuration and summarise it" in {
    val (configs, _) = store()
    val result = configs.validate(SampleConfig.yaml())
    assert(result.ok && result.error.isEmpty)
    val summary = result.summary.get
    assert(summary.periodStart == "2002-01-05" && summary.sites.map(_.patch) == Seq(101))
    assert(summary.sites.head.larvae == 100)
  }

  it should "explain broken YAML without dumping the parser's output" in {
    val (configs, _) = store()
    val result = configs.validate("settings: [unclosed")
    assert(!result.ok)
    val message = result.error.get
    assert(message.startsWith("The file is not valid YAML"))
    assert(message.contains("line 1") && !message.contains("\n"))
  }

  it should "say which field has the wrong type" in {
    val (configs, _) = store()
    val broken = SampleConfig.yaml().replace("randomSeed: 1234", "randomSeed: abc")
    assert(configs.validate(broken).error.contains("settings.randomSeed must be a whole number."))
  }

  it should "say which section is missing" in {
    val (configs, _) = store()
    val broken = SampleConfig.yaml().replace("turbulence:", "turbulenceX:")
    assert(configs.validate(broken).error.exists(_.contains("turbulence is missing")))
  }

  it should "reject a date that cannot be read" in {
    val (configs, _) = store()
    val result = configs.validate(SampleConfig.yaml(releaseStart = "not-a-date"))
    assert(!result.ok && result.error.exists(_.contains("'not-a-date' is not a valid date")))
  }

  it should "warn about missing input files" in {
    val (configs, _) = store()
    val warnings = configs.validate(SampleConfig.yaml()).warnings
    assert(warnings.exists(_.contains("The flow folder 'flow' does not exist")))
    assert(warnings.exists(_.contains("habitat shapefile")))
  }

  it should "not warn about input files that are there" in {
    val (configs, base) = store()
    Seq("flow/u", "flow/v", "flow/w", "habitat").foreach(d => Files.createDirectories(base.resolve(d)))
    Files.writeString(base.resolve("habitat/reefs.shp"), "x")
    val warnings = configs.validate(SampleConfig.yaml()).warnings
    assert(!warnings.exists(w => w.contains("flow folder") || w.contains("shapefile")), warnings.toString)
  }

  it should "warn when the flow period starts before the first release, which ends the run at once" in {
    val (configs, _) = store()
    val warnings = configs.validate(SampleConfig.yaml(periodStart = "2002-01-01")).warnings
    assert(warnings.exists(_.contains("end immediately")))
    assert(!configs.validate(SampleConfig.yaml()).warnings.exists(_.contains("end immediately")))
  }

  it should "accept any release interval of at least a day" in {
    val (configs, _) = store()
    for (interval <- Seq(1, 3, 5, 7, 14)) {
      val result = configs.validate(SampleConfig.yaml(interval = interval))
      assert(result.ok && !result.warnings.exists(_.contains("interval")), s"interval $interval")
    }
  }

  it should "reject a release interval that would divide by zero" in {
    val (configs, _) = store()
    for (interval <- Seq(0, -3)) {
      val result = configs.validate(SampleConfig.yaml(interval = interval))
      assert(!result.ok && result.error.exists(_.contains("must be at least 1 day")), s"interval $interval")
    }
  }

  it should "warn about a site outside the supported grid" in {
    val (configs, _) = store()
    assert(configs.validate(SampleConfig.yaml(latitude = -60.0)).warnings.exists(_.contains("outside the supported flow grid")))
  }

  it should "warn that pldType is case-sensitive" in {
    val (configs, _) = store()
    assert(configs.validate(SampleConfig.yaml(pldType = "Fixed")).warnings.exists(_.contains("case-sensitive")))
    assert(!configs.validate(SampleConfig.yaml(pldType = "fixed")).warnings.exists(_.contains("case-sensitive")))
  }

  it should "reject a negative diffusion coefficient, which has no square root" in {
    val (configs, _) = store()
    for (name <- Seq("horizontalDiffusionCoefficient", "verticalDiffusionCoefficient")) {
      val broken = SampleConfig.yaml().replaceAll(s"$name: [0-9.]+", s"$name: -1")
      val result = configs.validate(broken)
      assert(!result.ok && result.error.exists(_.contains(s"turbulence.$name must be zero or more")), name)
    }
    assert(configs.validate(SampleConfig.yaml().replaceAll("horizontalDiffusionCoefficient: [0-9.]+", "horizontalDiffusionCoefficient: 0")).ok, "zero is allowed")
  }

  it should "reject a mortality rate that is a percentage, or missing, for mortal larvae" in {
    val (configs, _) = store()
    val percentage = configs.validate(SampleConfig.yaml().replace("mortalityRate: 0.26", "mortalityRate: 26"))
    assert(!percentage.ok && percentage.error.exists(_.contains("from 0 to 1")))
    val missing = configs.validate(SampleConfig.yaml().replace("  mortalityRate: 0.26\n", ""))
    assert(!missing.ok && missing.error.exists(_.contains("larva.mortalityRate is missing")))
    assert(configs.validate(SampleConfig.yaml().replace("isMortal: true", "isMortal: false").replace("  mortalityRate: 0.26\n", "")).ok,
      "an immortal larva needs no rate")
  }

  it should "reject migration probabilities that do not add up to 1" in {
    val (configs, _) = store()
    def withDiel(day: String) = SampleConfig.yaml().replace(
      "  pelagicLarvalDuration:\n",
      s"  dielProbabilities:\n    depths: [25, 50]\n    day: $day\n    night: [0.5, 0.5]\n  pelagicLarvalDuration:\n"
    )
    assert(configs.validate(withDiel("[0.3, 0.7]")).ok)
    val result = configs.validate(withDiel("[30, 70]"))
    assert(!result.ok && result.error.contains("larva.dielProbabilities.day must add up to 1, not 100."))
  }
}

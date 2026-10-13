package tools

import java.io.File
import java.nio.file.Files
import io.config.{Configuration, ConfigurationFile}
import model.CoupledBiophysicalModel

/**
  * Runs the whole model on small generated inputs: the flow of UniformFlowDataset for January
  * and February 2002, which is gentle (a few km a day east and south), and one reef, patch 101,
  * covering the whole of its grid (153 to 154 east, 28.5 to 27.5 south), so a larva within the
  * settlement distance of it is a larva anywhere on the grid.
  *
  * The larva's traits are given as the `larva:` block of the configuration, so each test says
  * exactly which behaviours are switched on.
  */
object ModelFixture {

  /**
    * A larva that drifts and can settle after a day, and dies only of old age unless it is
    * given a daily mortality rate.
    */
  def driftingLarva(
      pld: Double = 3,
      nonSettlementPeriod: Double = 1,
      mortalityRate: Option[Double] = None,
      ontogeny: (Int, Int, Int) = (1, 2, 3),
      extra: String = ""
  ): String =
    s"""larva:
       |  species: fish
       |  isMortal: ${mortalityRate.isDefined}
       |  mortalityRate: ${mortalityRate.getOrElse(0.0)}
       |  ontogeny:
       |    hatching: 0
       |    preflexion: ${ontogeny._1}
       |    flexion: ${ontogeny._2}
       |    postflexion: ${ontogeny._3}
       |  pelagicLarvalDuration:
       |    mean: $pld
       |    stdev: 0
       |    pldType: fixed
       |    nonSettlementPeriod: $nonSettlementPeriod
       |$extra""".stripMargin

  def yaml(
      root: File,
      larva: String,
      numberOfLarvae: Int = 50,
      releaseStart: String = "2002-01-05",
      releaseEnd: String = "2002-01-06",
      interval: Int = 1,
      periodStart: String = "2002-01-05",
      periodEnd: String = "2002-01-20",
      timeStep: String = "unit: Hour\n    duration: 2",
      // the (latitude, longitude) of each release site, in the order the configuration lists them
      sites: Seq[(Double, Double)] = Seq((-28.0, 153.5)),
      depth: Double = 5,
      settlement: Double = 5,
      olfactory: Double = 10,
      includeLarvaeHistory: Boolean = false
  ): String = {
    val spawningLocations = sites.zipWithIndex.map { case ((latitude, longitude), i) =>
      s"""    - name: site ${i + 1}
         |      patchNumber: 101
         |      site:
         |        longitude: $longitude
         |        latitude: $latitude
         |        depth: $depth
         |      numberOfLarvae: $numberOfLarvae
         |      releasePeriod:
         |        start: $releaseStart
         |        end: $releaseEnd
         |      interval: $interval
         |""".stripMargin
    }
    s"""settings:
       |  randomSeed: 1234
       |spawn:
       |  spawningLocation:
       |${spawningLocations.mkString.stripLineEnd}
       |turbulence:
       |  horizontalDiffusionCoefficient: 0
       |  verticalDiffusionCoefficient: 0
       |  applyTurbulence: false
       |  interval: 1
       |${larva.stripLineEnd}
       |flow:
       |  netcdfFilePath: ${new File(root, "flow")}
       |  period:
       |    start: $periodStart
       |    end: $periodEnd
       |  timeStep:
       |    $timeStep
       |  includeVerticalVelocity: false
       |habitat:
       |  shapeFilePath: ${new File(root, "reefs.shp")}
       |  buffer:
       |    settlement: $settlement
       |    olfactory: $olfactory
       |output:
       |  includeLarvaeHistory: $includeLarvaeHistory
       |  saveOutputFilePath: ${new File(root, "output")}
       |  percentage: 100
       |  prefix: fixture
       |  logLevel: info
       |  logFile: ""
       |""".stripMargin
  }

  /** A folder holding the flow files and the reef. */
  def inputs(): File = {
    val root = Files.createTempDirectory("zissou-model-test").toFile
    UniformFlowDataset.write(new File(root, "flow"), Seq((2002, 1), (2002, 2)))
    ShapefileTestUtil.writeReefs(
      new File(root, "reefs.shp"),
      Seq((101, "Reef", Seq((153.0, -28.5), (154.0, -28.5), (154.0, -27.5), (153.0, -27.5))))
    )
    new File(root, "output").mkdirs()
    root
  }

  def configuration(text: String): Configuration =
    new ConfigurationFile().parse(text).fold(e => throw e, identity)

  /** Runs the model to the end and hands it back, to look at its larvae. */
  def run(text: String): CoupledBiophysicalModel = {
    val model = new CoupledBiophysicalModel(configuration(text), "fixture")
    model.run()
    model
  }
}

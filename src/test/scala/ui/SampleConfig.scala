package ui

/** A complete, valid configuration, for tests that need one. */
object SampleConfig {

  def yaml(
      flow: String = "flow",
      habitat: String = "habitat/reefs.shp",
      periodStart: String = "2002-01-05",
      periodEnd: String = "2002-02-20",
      releaseStart: String = "2002-01-05",
      releaseEnd: String = "2002-01-20",
      interval: Int = 7,
      latitude: Double = -28.8,
      pldType: String = "fixed"
  ): String =
    s"""settings:
       |  randomSeed: 1234
       |spawn:
       |  spawningLocation:
       |    - name: patch 101
       |      patchNumber: 101
       |      site:
       |        longitude: 153.7
       |        latitude: $latitude
       |        depth: 5
       |      numberOfLarvae: 100
       |      releasePeriod:
       |        start: $releaseStart
       |        end: $releaseEnd
       |      interval: $interval
       |turbulence:
       |  horizontalDiffusionCoefficient: 300
       |  verticalDiffusionCoefficient: 15
       |  applyTurbulence: true
       |  interval: 1
       |larva:
       |  species: fish
       |  isMortal: true
       |  mortalityRate: 0.26
       |  ontogeny:
       |    hatching: 0
       |    preflexion: 2
       |    flexion: 5
       |    postflexion: 8
       |  pelagicLarvalDuration:
       |    mean: 15
       |    stdev: 1.5
       |    pldType: $pldType
       |    nonSettlementPeriod: 5
       |flow:
       |  netcdfFilePath: $flow
       |  period:
       |    start: $periodStart
       |    end: $periodEnd
       |  timeStep:
       |    unit: Hour
       |    duration: 2
       |  includeVerticalVelocity: true
       |habitat:
       |  shapeFilePath: $habitat
       |  buffer:
       |    settlement: 5
       |    olfactory: 10
       |output:
       |  includeLarvaeHistory: false
       |  saveOutputFilePath: somewhere
       |  percentage: 5
       |  prefix: test
       |  logLevel: info
       |  logFile: some.log
       |""".stripMargin
}

package io.config

import locals._
case class Configuration(
    settings: SettingsConfig,
    spawn: SpawnConfig,
    turbulence: TurbulenceConfig,
    larva: LarvaConfig,
    flow: FlowConfig,
    habitat: HabitatConfig,
    output: OutputFilesConfig
)

case class SettingsConfig(
    randomSeed: Int
)

case class SpawningLocationConfig(
    name: String,
    patchNumber: Int,
    site: SiteConfig,
    numberOfLarvae: Int,
    releasePeriod: ReleasePeriodConfig,
    interval: Int
)

case class SpawnConfig(spawningLocation: List[SpawningLocationConfig])

case class SiteConfig(
    longitude: Double,
    latitude: Double,
    depth: Double,
    flowId: Option[Int]
)

case class ReleasePeriodConfig(start: String, end: String)

case class TurbulenceConfig(
    horizontalDiffusionCoefficient: Double,
    verticalDiffusionCoefficient: Double,
    applyTurbulence: Boolean,
    interval: Int
)

case class FlowConfig(
    netcdfFilePath: String,
    period: PeriodConfig,
    timeStep: TimeStepConfig,
    includeVerticalVelocity: Boolean
)

case class PeriodConfig(start: String, end: String)

case class TimeStepConfig(unit: String, duration: Int)

case class HabitatConfig(shapeFilePath: String, buffer: BufferConfig)

case class BufferConfig(
    settlement: Double,
    olfactory: Double
)

case class OutputFilesConfig(
    includeLarvaeHistory: Boolean,
    saveOutputFilePath: String,
    percentage: Int,
    prefix: String,
    logLevel: String,
    logFile: String
)

case class LarvaConfig(
    species: String,
    ontogeny: OntogenyConfig,
    swimming: Option[SwimmingConfig],
    ovmProbabilities: Option[OntogeneticMigrationConfig],
    dielProbabilities: Option[DielMigrationConfig],
    pelagicLarvalDuration: PelagicLarvalDurationConfig,
    isMortal: Option[Boolean],
    mortalityRate: Option[Double]
)

case class OntogenyConfig(
    hatching: Int,
    preflexion: Int,
    flexion: Int,
    postflexion: Int
)

case class SwimmingConfig(
    strategy: String,
    ability: Option[String],
    criticalSwimmingSpeed: Option[Double],
    inSituSwimmingPotential: Option[Double],
    endurance: Option[Double],
    reynoldsEffect: Option[Boolean],
    hatchSwimmingSpeed: Option[Double]
)

case class PelagicLarvalDurationConfig(
    mean: Double,
    stdev: Double,
    pldType: String,
    nonSettlementPeriod: Double
)

case class OntogeneticMigrationConfig(
    implementation: String,
    depths: List[Int],
    hatching: List[Double],
    preflexion: List[Double],
    flexion: List[Double],
    postflexion: List[Double]
)

case class DielMigrationConfig(
    depths: List[Double],
    day: List[Double],
    night: List[Double]
)

package io.config

import locals._

case class Configuration(
    inputFiles: InputFilesConfig,
    spawn: SpawnConfig,
    turbulence: TurbulenceConfig,
    larva: LarvaConfig,
    flow: FlowConfig,
    habitat: HabitatConfig,
    output: OutputFilesConfig
)

case class InputFilesConfig(
    pathNetcdfFiles: String,
    pathHabitatShapeFile: String,
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
    period: PeriodConfig,
    timeStep: TimeStepConfig,
    depth: DepthConfig,
    includeVerticalVelocity: Boolean
)

case class PeriodConfig(start: String, end: String)

case class TimeStepConfig(unit: String, duration: Int)

case class DepthConfig(
    average: Boolean,
    averageOverAllDepths: Boolean,
    maximumDepthForAverage: Int
)

case class HabitatConfig(buffer: BufferConfig)

case class BufferConfig(
    isBuffered: Boolean,
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
    swimming: SwimmingConfig,
    ovmProbabilities: OntogeneticMigrationConfig,
    dielProbabilities: VerticalMigrationDielConfig,
    pelagicLarvalDuration: PelagicLarvalDurationConfig,
    isMortal: Boolean,
    mortalityRate: Double
)

case class OntogenyConfig(
    hatching: Int,
    preflexion: Int,
    flexion: Int,
    postflexion: Int
)

case class SwimmingConfig(
    strategy: String,
    ability: String,
    criticalSwimmingSpeed: Double,
    inSituSwimmingPotential: Double,
    endurance: Double,
    reynoldsEffect: Boolean,
    ageMaxSpeedReached: Int,
    hatchSwimmingSpeed: Double
)

case class PelagicLarvalDurationConfig(
    mean: Double,
    stdev: Double,
    pldType: String,
    nonSettlementPeriod: Double
)

case class OntogeneticMigrationConfig(
    implementation: String,
    verticalMigrationOntogeneticProbability: List[
      OntogeneticMigrationProbabilityConfig
    ]
)

case class OntogeneticMigrationProbabilityConfig(
    depthStart: Int,
    depthFinish: Int,
    hatching: Double,
    preflexion: Double,
    flexion: Double,
    postflexion: Double
)

case class VerticalMigrationDielProbabilityConfig(
    depthStart: Int,
    depthFinish: Int,
    day: Double,
    night: Double
)

case class VerticalMigrationDielConfig(
    verticalMigrationDielProbability: List[
      VerticalMigrationDielProbabilityConfig
    ]
)

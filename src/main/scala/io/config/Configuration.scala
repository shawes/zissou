package io.config

import io.circe.Decoder
import locals._
case class Configuration(
    settings: SettingsConfig,
    spawn: SpawnConfig,
    turbulence: TurbulenceConfig,
    larva: LarvaConfig,
    flow: FlowConfig,
    habitat: HabitatConfig,
    output: OutputFilesConfig
) derives Decoder

case class SettingsConfig(
    randomSeed: Int
) derives Decoder

case class SpawningLocationConfig(
    name: String,
    patchNumber: Int,
    site: SiteConfig,
    numberOfLarvae: Int,
    releasePeriod: ReleasePeriodConfig,
    interval: Int
) derives Decoder

case class SpawnConfig(spawningLocation: List[SpawningLocationConfig]) derives Decoder

case class SiteConfig(
    longitude: Double,
    latitude: Double,
    depth: Double,
    flowId: Option[Int]
) derives Decoder

case class ReleasePeriodConfig(start: String, end: String) derives Decoder

case class TurbulenceConfig(
    horizontalDiffusionCoefficient: Double,
    verticalDiffusionCoefficient: Double,
    applyTurbulence: Boolean,
    interval: Int
) derives Decoder

case class FlowConfig(
    netcdfFilePath: String,
    period: PeriodConfig,
    timeStep: TimeStepConfig,
    includeVerticalVelocity: Boolean,
    bounds: Option[BoundsConfig],
    depthLevels: Option[Int],
    filePrefix: Option[String],
    nested: Option[List[NestedFlowConfig]]
) derives Decoder

/**
  * A finer ocean model inside the main one, whose monthly files are laid out like the main model's.
  * Its period and time step are the main flow's, and its depth levels are too unless it gives its own.
  */
case class NestedFlowConfig(
    netcdfFilePath: String,
    bounds: BoundsConfig,
    depthLevels: Option[Int],
    filePrefix: Option[String]
) derives Decoder

case class BoundsConfig(south: Double, north: Double, west: Double, east: Double) derives Decoder

case class PeriodConfig(start: String, end: String) derives Decoder

case class TimeStepConfig(unit: String, duration: Int) derives Decoder

case class HabitatConfig(shapeFilePath: String, buffer: BufferConfig) derives Decoder

case class BufferConfig(
    settlement: Double,
    olfactory: Double
) derives Decoder

case class OutputFilesConfig(
    includeLarvaeHistory: Boolean,
    saveOutputFilePath: String,
    percentage: Int,
    prefix: String,
    logLevel: String,
    logFile: String
) derives Decoder

case class LarvaConfig(
    species: String,
    ontogeny: OntogenyConfig,
    swimming: Option[SwimmingConfig],
    ovmProbabilities: Option[OntogeneticMigrationConfig],
    dielProbabilities: Option[DielMigrationConfig],
    pelagicLarvalDuration: PelagicLarvalDurationConfig,
    isMortal: Option[Boolean],
    mortalityRate: Option[Double]
) derives Decoder

case class OntogenyConfig(
    // read, so that configurations that give it still load, but not used: a larva is in
    // its first stage from birth until preflexion
    hatching: Option[Int],
    preflexion: Int,
    flexion: Int,
    postflexion: Int
) derives Decoder

case class SwimmingConfig(
    strategy: String,
    ability: Option[String],
    criticalSwimmingSpeed: Option[Double],
    inSituSwimmingPotential: Option[Double],
    endurance: Option[Double],
    reynoldsEffect: Option[Boolean],
    hatchSwimmingSpeed: Option[Double]
) derives Decoder

case class PelagicLarvalDurationConfig(
    mean: Double,
    stdev: Double,
    pldType: String,
    nonSettlementPeriod: Double
) derives Decoder

case class OntogeneticMigrationConfig(
    implementation: String,
    depths: List[Int],
    hatching: List[Double],
    preflexion: List[Double],
    flexion: List[Double],
    postflexion: List[Double]
) derives Decoder

case class DielMigrationConfig(
    depths: List[Double],
    day: List[Double],
    night: List[Double]
) derives Decoder

package io.config

import java.io.File

import biology._
import biology.fish._
import biology.swimming._
import com.github.nscala_time.time.Imports._
import io.{InputFiles, OutputFiles}
import locals._
import maths.ContinuousRange
import org.apache.commons.math3.distribution.NormalDistribution
import physical.flow.{Depth, Flow}
import physical.habitat.Buffer
import physical.{GeoCoordinate, TimeStep}
import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
  * The config mappings object has implicit definitions to map each config class to its corresponding object
  */
object ConfigMappings {
  implicit def flowConfigToFlow(f: FlowConfig): Flow =
    new Flow(
      new Depth(f.depth),
      new DateTime(f.period.start, DateTimeZone.UTC) to new DateTime(
        f.period.end,
        DateTimeZone.UTC
      ),
      f.timeStep,
      f.includeVerticalVelocity
    )

  implicit def timeStepConfigToTimeStep(t: TimeStepConfig): TimeStep =
    new TimeStep(t.duration, TimeStepType.withName(t.unit))

  implicit def bufferConfigMap(b: BufferConfig): Buffer =
    new Buffer(b.isBuffered, b.settlement, b.olfactory)

  implicit def inputConfigMap(i: InputFilesConfig): InputFiles = {
    val seed: Option[Int] = if (i.randomSeed > 0) Some(i.randomSeed) else None
    new InputFiles(
      i.pathNetcdfFiles,
      i.pathHabitatShapeFile,
      new File(i.pathNetcdfFiles).list(),
      seed
    )
  }

  implicit def depthConfigMap(d: DepthConfig): Depth =
    new Depth(d.average, d.averageOverAllDepths, d.maximumDepthForAverage, null)

  implicit def outputFilesConfigMap(o: OutputFilesConfig): OutputFiles =
    new OutputFiles(
      o.includeLarvaeHistory,
      o.saveOutputFilePath,
      o.prefix,
      o.percentage
    )

  implicit def releasePeriodConfigMap(r: ReleasePeriodConfig): Interval =
    new DateTime(r.start, DateTimeZone.UTC) to new DateTime(
      r.end,
      DateTimeZone.UTC
    )

  implicit def spawnConfigMap(s: SpawnConfig): List[SpawningLocation] =
    s.spawningLocation.map(x => spawningLocationConfigMap(x)).toList

  implicit def spawningLocationConfigMap(
      s: SpawningLocationConfig
  ): SpawningLocation =
    new SpawningLocation(
      s.name,
      s.numberOfLarvae,
      s.patchNumber,
      s.site,
      s.releasePeriod,
      s.interval
    )

  implicit def siteConfigMap(s: SiteConfig): GeoCoordinate =
    new GeoCoordinate(s.latitude, s.longitude, s.depth)

  implicit def pelagicLarvalDurationMap(
      pld: PelagicLarvalDurationConfig
  ): PelagicLarvalDuration =
    new PelagicLarvalDuration(
      new NormalDistribution(pld.mean, pld.stdev),
      pld.pldType match {
        case "fixed" => true
        case _       => false
      },
      pld.nonSettlementPeriod
    )

  implicit def swimmingConfigMap(s: SwimmingConfig): HorizontalSwimmingConfig =
    new HorizontalSwimmingConfig(
      s.ability match {
        case "directed"   => Directed
        case "undirected" => Undirected
        case _            => Passive
      },
      s.strategy match {
        case _ => StrategyOne
      },
      s.criticalSwimmingSpeed,
      s.inSituSwimmingPotential,
      s.endurance,
      s.reynoldsEffect,
      s.ageMaxSpeedReached,
      s.hatchSwimmingSpeed
    )

  implicit def verticalMigrationOntogeneticConfigMap(
      vm: VerticalMigrationOntogeneticConfig
  ): VerticalMigrationOntogenetic =
    new VerticalMigrationOntogenetic(
      vm.implementation match {
        case "Daily"    => DailyMigration
        case "Timestep" => TimeStepMigration
        case _          => StageMigration
      },
      vm.verticalMigrationOntogeneticProbability
        .map(x => verticalMigrationOntogeneticProbabilityConfigMap(x))
        .toList
    )

  implicit def verticalMigrationDielConfigMap(
      vm: VerticalMigrationDielConfig
  ): VerticalMigrationDiel =
    new VerticalMigrationDiel(
      vm.verticalMigrationDielProbability
        .map(x => verticalMigrationDielProbabilityConfigMap(x))
        .toList
    )

  implicit def verticalMigrationOntogeneticProbabilityConfigMap(
      prob: VerticalMigrationOntogeneticProbabilityConfig
  ): VerticalMigrationOntogeneticProbability =
    new VerticalMigrationOntogeneticProbability(
      new ContinuousRange(prob.depthStart, prob.depthFinish, true),
      prob.hatching,
      prob.preflexion,
      prob.flexion,
      prob.postflexion
    )

  implicit def verticalMigrationDielProbabilityConfigMap(
      prob: VerticalMigrationDielProbabilityConfig
  ): VerticalMigrationDielProbability =
    new VerticalMigrationDielProbability(
      new ContinuousRange(prob.depthStart, prob.depthFinish, true),
      prob.day,
      prob.night
    )

}

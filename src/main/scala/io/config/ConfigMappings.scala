package io.config

import java.io.File

import biology._
import biology.fish._
import biology.swimming._
import com.github.nscala_time.time.Imports._
import io.OutputFiles
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
      f.netcdfFilePath,
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
    new Buffer(b.settlement, b.olfactory)

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

  // implicit def swimmingConfigMap(s: SwimmingConfig): HorizontalSwimmingConfig =
  //   new HorizontalSwimmingConfig(
  //     s.ability match {
  //       case "directed"   => Directed
  //       case "undirected" => Undirected
  //       case _            => Passive
  //     },
  //     s.strategy match {
  //       case _ => StrategyOne
  //     },
  //     s.criticalSwimmingSpeed,
  //     s.inSituSwimmingPotential,
  //     s.endurance,
  //     s.reynoldsEffect,
  //     s.ageMaxSpeedReached,
  //     s.hatchSwimmingSpeed
  //   )

  implicit def verticalMigrationOntogeneticConfigMap(
      vm: Option[OntogeneticMigrationConfig]
  ): Option[OntogeneticMigration] =
    vm match {
      case Some(vm) =>
        Some(
          new OntogeneticMigration(
            vm.implementation match {
              case "Daily"    => DailyMigration
              case "TimeStep" => TimeStepMigration
              case _          => OntogeneticStageMigration
            },
            vm.ontogeneticMigrationProbability
              .map(x => verticalMigrationOntogeneticProbabilityConfigMap(x))
              .toList
          )
        )
      case None => None
    }

  implicit def verticalMigrationDielConfigMap(
      vm: Option[DielMigrationConfig]
  ): Option[DielMigration] =
    vm match {
      case Some(vm) =>
        Some(
          new DielMigration(
            vm.dielMigrationProbability
              .map(x => verticalMigrationDielProbabilityConfigMap(x))
              .toList
          )
        )
      case None => None
    }

  implicit def verticalMigrationOntogeneticProbabilityConfigMap(
      probability: OntogeneticMigrationProbabilityConfig
  ): OntogeneticMigrationProbability =
    new OntogeneticMigrationProbability(
      new ContinuousRange(
        probability.depthStart,
        probability.depthFinish,
        true
      ),
      probability.hatching,
      probability.preflexion,
      probability.flexion,
      probability.postflexion
    )

  implicit def verticalMigrationDielProbabilityConfigMap(
      probability: DielMigrationProbabilityConfig
  ): DielMigrationProbability =
    new DielMigrationProbability(
      new ContinuousRange(
        probability.depthStart,
        probability.depthFinish,
        true
      ),
      probability.day,
      probability.night
    )

}

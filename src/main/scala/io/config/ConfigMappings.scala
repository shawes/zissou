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
import physical.flow.{Flow, FlowRegion}
import physical.habitat.Buffer
import physical.{GeoCoordinate, TimeStep}
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/**
  * The config mappings object has implicit definitions to map each config class to its corresponding object
  */
object ConfigMappings {
  implicit def flowConfigToFlow(f: FlowConfig): Flow = {
    val period = new DateTime(f.period.start, DateTimeZone.UTC) to new DateTime(f.period.end, DateTimeZone.UTC)
    val timeStep: TimeStep = f.timeStep
    val depthLevels = f.depthLevels.getOrElse(FlowRegion.DefaultDepthLevels)
    new Flow(
      f.netcdfFilePath,
      period,
      timeStep,
      f.includeVerticalVelocity,
      f.bounds.map(b => FlowRegion(b.south, b.north, b.west, b.east)).getOrElse(FlowRegion.Default),
      depthLevels,
      f.filePrefix.getOrElse("ocean"),
      f.nested.getOrElse(List.empty).map { n =>
        new Flow(
          n.netcdfFilePath,
          period,
          timeStep,
          f.includeVerticalVelocity,
          FlowRegion(n.bounds.south, n.bounds.north, n.bounds.west, n.bounds.east),
          n.depthLevels.getOrElse(depthLevels),
          n.filePrefix.getOrElse("ocean")
        )
      }
    )
  }

  implicit def timeStepConfigToTimeStep(t: TimeStepConfig): TimeStep =
    new TimeStep(t.duration, TimeStepType.withName(t.unit))

  implicit def bufferConfigMap(b: BufferConfig): Buffer =
    new Buffer(b.settlement, b.olfactory)

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

}

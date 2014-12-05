package io.config

import java.io.File

import biology._
import com.github.nscala_time.time.Imports._
import io.InputFiles
import locals._
import maths.NormalDistribution
import org.joda.time.DateTime
import physical.flow.{Depth, Flow}
import physical.habitat.Buffer
import physical.{GeoCoordinate, Grid, TimeStep}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.language.implicitConversions

/**
 * The config mappings object has implicit def's to map each config class to its corresponding object
 */
object ConfigMappings {
  implicit def flowConfigToFlow(f: FlowConfig): Flow =
    new Flow(new Grid(), null, null, new Depth(f.depth), new Interval(new DateTime(f.period.start), new DateTime(f.period.end)), f.timeStep)

  implicit def timeStepConfigToTimeStep(t: TimeStepConfig): TimeStep =
    new TimeStep(t.duration, TimeStepType.withName(t.unit))

  implicit def bufferConfigMap(b: BufferConfig): Buffer =
    new Buffer(b.isBuffered, b.bufferSize, b.filePath)

  implicit def inputConfigMap(i: InputFilesConfig): InputFiles =
    new InputFiles(i.flowFilePath, i.habitatFilePath, new File(i.flowFilePath).list())

  implicit def depthConfigMap(d: DepthConfig): Depth =
    new Depth(d.average, d.averageOverAllDepths, d.maximumDepthForAverage, null)

  implicit def releasePeriodConfigMap(r: ReleasePeriodConfig): Interval =
    new Interval(new DateTime(r.start), new DateTime(r.end))

  implicit def spawnConfigMap(s: SpawnConfig): mutable.Buffer[SpawningLocation] =
    s.spawningLocation.map(x => spawningLocationConfigMap(x))

  implicit def spawningLocationConfigMap(s: SpawningLocationConfig): SpawningLocation =
    new SpawningLocation(s.name, s.numberOfLarvae, s.site, s.releasePeriod, s.interval)

  implicit def siteConfig(s: SiteConfig): GeoCoordinate = new GeoCoordinate(s.latitude, s.longitude, s.depth)

  implicit def pelagicLarvalDurationMap(pld: PelagicLarvalDurationConfig): PelagicLarvalDuration =
    new PelagicLarvalDuration(new NormalDistribution(pld.mean, pld.stdev), DistributionType.Normal)

  implicit def ontogenyConfigMap(o: OntogenyConfig): Ontogeny = new Ontogeny(o.preFlexion, o.flexion, o.postFlexion)

  implicit def verticalMigrationConfigMap(vm: VerticalMigrationConfig): VerticalMigration =
    new VerticalMigration(vm.verticalMigrationProbability.map(x => verticalMigrationProbabilityConfigMap(x)).toList)

  implicit def verticalMigrationProbabilityConfigMap(prob: VerticalMigrationProbabilityConfig) =
    new VerticalMigrationProbability(prob.depth, prob.hatching, prob.preFlexion, prob.flexion, prob.postFlexion)

  implicit def fishConfigMap(f: FishConfig): Fish = new Fish(f.pelagicLarvalDuration, f.ontogeny, "name", true, SwimmingAbility.withName(f.swimmingAbility), f.meanSwimmingSpeed,
    VerticalMigrationPattern.withName(f.verticalMigrationPattern), f.verticalMigrationProbabilities, f.isMortal, f.mortalityRate)
}

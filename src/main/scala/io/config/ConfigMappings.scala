package io.config

import java.io.File

import biology._
import biology.fish._
import com.github.nscala_time.time.Imports._
import io.{InputFiles, OutputFiles}
import locals._
import maths.{ContinuousRange, NormalDistribution, Time}
import org.joda.time.DateTime
import physical.flow.{Depth, Flow}
import physical.habitat.Buffer
import physical.{GeoCoordinate, TimeStep}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
 * The config mappings object has implicit def's to map each config class to its corresponding object
 */
object ConfigMappings {
  implicit def flowConfigToFlow(f: FlowConfig): Flow =
    new Flow(new Depth(f.depth), new Interval(new DateTime(f.period.start), new DateTime(f.period.end)), f.timeStep)

  implicit def timeStepConfigToTimeStep(t: TimeStepConfig): TimeStep =
    new TimeStep(t.duration, TimeStepType.withName(t.unit))

  implicit def bufferConfigMap(b: BufferConfig): Buffer =
    new Buffer(b.isBuffered, b.bufferSize)

  implicit def inputConfigMap(i: InputFilesConfig): InputFiles =
    new InputFiles(i.flowFilePath, i.habitatFilePath, new File(i.flowFilePath).list())

  implicit def depthConfigMap(d: DepthConfig): Depth =
    new Depth(d.average, d.averageOverAllDepths, d.maximumDepthForAverage, null)

  implicit def outputFilesConfigMap(o: OutputFilesConfig): OutputFiles =
    new OutputFiles(o.includeLarvaeHistory, ShapeFileType.withName(o.shape), o.saveOutputFilePath)

  implicit def releasePeriodConfigMap(r: ReleasePeriodConfig): Interval =
    new Interval(new DateTime(r.start), new DateTime(r.end))

  implicit def spawnConfigMap(s: SpawnConfig): List[SpawningLocation] =
    s.spawningLocation.asScala.map(x => spawningLocationConfigMap(x)).toList

  implicit def spawningLocationConfigMap(s: SpawningLocationConfig): SpawningLocation =
    new SpawningLocation(s.name, s.numberOfLarvae, s.site, s.releasePeriod, s.interval)

  implicit def siteConfigMap(s: SiteConfig): GeoCoordinate = new GeoCoordinate(s.latitude, s.longitude, s.depth)

  implicit def pelagicLarvalDurationMap(pld: PelagicLarvalDurationConfig): PelagicLarvalDuration =
    new PelagicLarvalDuration(new NormalDistribution(pld.mean, pld.stdev), DistributionType.Normal)

  implicit def ontogenyConfigMap(o: OntogenyConfig): FishOntogeny = new FishOntogeny(Time.convertDaysToSeconds(o.preFlexion),
    Time.convertDaysToSeconds(o.flexion), Time.convertDaysToSeconds(o.postFlexion))

  implicit def verticalMigrationOntogeneticConfigMap(vm: VerticalMigrationOntogeneticConfig): FishVerticalMigrationOntogenetic =
    new FishVerticalMigrationOntogenetic(vm.fishVerticalMigrationOntogeneticProbability.asScala.map(x => verticalMigrationOntogeneticProbabilityConfigMap(x)).toList)

  implicit def verticalMigrationDielConfigMap(vm: VerticalMigrationDielConfig): VerticalMigrationDiel =
    new VerticalMigrationDiel(vm.verticalMigrationDielProbability.asScala.map(x => verticalMigrationDielProbabilityConfigMap(x)).toList)

  implicit def verticalMigrationOntogeneticProbabilityConfigMap(prob: VerticalMigrationOntogeneticProbabilityConfig) :  FishVerticalMigrationOntogeneticProbability =
    new FishVerticalMigrationOntogeneticProbability(new ContinuousRange(prob.depthStart, prob.depthFinish, true), prob.hatching, prob.preFlexion, prob.flexion, prob.postFlexion)

  implicit def verticalMigrationDielProbabilityConfigMap(prob: VerticalMigrationDielProbabilityConfig) : VerticalMigrationDielProbability =
    new VerticalMigrationDielProbability(new ContinuousRange(prob.depthStart, prob.depthFinish, true), prob.day, prob.night)

  implicit def fishConfigMap(f: FishConfig): FishParameters = new FishParameters(f.pelagicLarvalDuration, f.ontogeny, "name", true, SwimmingAbility.withName(f.swimmingAbility), f.meanSwimmingSpeed, f.verticalMigrationOntogeneticProbabilities, f.verticalMigrationDielProbabilities, f.isMortal, f.mortalityRate)
}

package io.config

import java.io.File

import biology.SpawningLocation
import com.github.nscala_time.time.Imports._
import io.InputFiles
import locals.TimeStepType
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
    new SpawningLocation(s.name, s.numberOfLarvae, new GeoCoordinate(s.site.latitude, s.site.longitude), s.releasePeriod, s.interval)
}

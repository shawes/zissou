package io.config

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.yaml

import org.scalatest.FlatSpec
import org.scalatestplus.mockito.MockitoSugar

class ConfigurationTest extends FlatSpec with MockitoSugar {

  private val configYAML = """
      inputFiles:
        pathNetcdfFiles: test1
        pathHabitatShapeFile: test2
        randomSeed: 1234
      spawn:
        spawningLocation:
         -
          name: location one
          patchNumber: 101
          site:
           longitude: 153.701
           latitude: -28.210
           depth: 5
          numberOfLarvae: 1000
          releasePeriod:
           start: 2010-07-01
           end: 2011-07-01
          interval: 7
         -
          name: location two
          patchNumber: 210
          site:
           longitude: 153.701
           latitude: -28.259
           depth: 5
          numberOfLarvae: 1000
          releasePeriod:
           start: 2010-07-01
           end: 2011-07-01
          interval: 7
      turbulence:
        horizontalDiffusionCoefficient: 300
        verticalDiffusionCoefficient: 15
        applyTurbulence: true
        interval: 1
      fish:
        isMortal: true
        mortalityRate: 0.26
        ontogeny:
         preFlexion: 0
         flexion: 5
         postFlexion: 8
        swimming:
         ability: Directed
         criticalSwimmingSpeed: 0.463
         inSituSwimmingPotential: 0.25
         endurance: 0.5
         reynoldsEffect: false
        pelagicLarvalDuration:
         mean: 18.3
         stdev: 1.5
         distribution: Normal
         pldType: Fixed
         nonSettlementPeriod: 5
        verticalMigrationDielProbabilities:
         verticalMigrationDielProbability:
          -
           depthStart: 0
           depthFinish: 25
           day: 0.1
           night: 0.3
          -
           depthStart: 26
           depthFinish: 50
           day: 0.3
           night: 0.3
          -
           depthStart: 51
           depthFinish: 75
           day: 0.4
           night: 0.3
          -
           depthStart: 76
           depthFinish: 100
           day: 0.2
           night: 0.1
        verticalMigrationOntogeneticProbabilities:
         implementation: Stage
         verticalMigrationOntogeneticProbability:
          -
           depthStart: 0
           depthFinish: 5
           hatching: 0
           preFlexion: 0.4
           flexion: 0.35
           postFlexion: 0.05
          -
           depthStart: 6
           depthFinish: 50
           hatching: 0
           preFlexion: 0.5
           flexion: 0.5
           postFlexion: 0.85
          -
           depthStart: 51
           depthFinish: 100
           hatching: 0
           preFlexion: 0.1
           flexion: 0.15
           postFlexion: 0.1
      flow:
        period:
         start: 2010-07-01
         end: 2011-08-31
        timeStep:
         unit: Hour
         duration: 2
        includeVerticalVelocity: true
        depth:
         average: false
         averageOverAllDepths: false
         maximumDepthForAverage: 5
      habitat:
        buffer:
         isBuffered: true
         settlement: 10
         olfactory: 10
      output:
        includeLarvaeHistory: false
        saveOutputFilePath: test3
        percentage: 5
        prefix: test4
        logLevel: info
        logFile: test.log
  """

  "The configuration case class" should "parse the YAML file into a configuration object" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.isInstanceOf[Configuration])
  }

  it should "parse the input files YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.inputFiles.pathNetcdfFiles == "test1")
    assert(config.inputFiles.pathHabitatShapeFile == "test2")
    assert(config.inputFiles.randomSeed == 1234)
  }

  it should "parse the spawning YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.spawn.spawningLocation.size == 2, "Not correct number of spawning locations")
    val actual = config.spawn.spawningLocation(0)
    assert(actual.name == "location one")
    assert(actual.numberOfLarvae == 1000)
    assert(actual.patchNumber == 101)
    assert(actual.interval == 7)
    assert(actual.site.latitude == -28.210)
    assert(actual.site.longitude == 153.701)
    assert(actual.site.depth == 5)
    assert(actual.releasePeriod.start == "2010-07-01")
    assert(actual.releasePeriod.end == "2011-07-01")
  }

  it should "parse the turbulence YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.turbulence.horizontalDiffusionCoefficient == 300)
    assert(config.turbulence.verticalDiffusionCoefficient == 15)
    assert(config.turbulence.applyTurbulence)
    assert(config.turbulence.interval == 1)
  }

  it should "parse the output YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(!config.output.includeLarvaeHistory)
    assert(config.output.saveOutputFilePath == "test3")
    assert(config.output.percentage == 5)
    assert(config.output.prefix == "test4")
    assert(config.output.logLevel == "info")
    assert(config.output.logFile == "test.log")
  }

  it should "parse the habitat YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.habitat.buffer.isBuffered)
    assert(config.habitat.buffer.settlement == 10)
    assert(config.habitat.buffer.olfactory == 10)
  }

  it should "parse the flow YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.flow.includeVerticalVelocity)
    assert(config.flow.period.start == "2010-07-01")
    assert(config.flow.period.end == "2011-08-31")
    assert(config.flow.timeStep.unit == "Hour")
    assert(config.flow.timeStep.duration == 2)
    assert(!config.flow.depth.average)
    assert(!config.flow.depth.averageOverAllDepths)
    assert(config.flow.depth.maximumDepthForAverage == 5)
  }

  it should "parse the ovm YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.fish.verticalMigrationOntogeneticProbabilities.implementation == "Stage")
    assert(config.fish.verticalMigrationOntogeneticProbabilities.verticalMigrationOntogeneticProbability.size == 3)
    val ontogenyProb = config.fish.verticalMigrationOntogeneticProbabilities.verticalMigrationOntogeneticProbability(0)

    assert(ontogenyProb.depthStart == 0)
    assert(ontogenyProb.depthFinish == 5)
    assert(ontogenyProb.hatching == 0)
    assert(ontogenyProb.preFlexion == 0.4)
    assert(ontogenyProb.flexion == 0.35)
    assert(ontogenyProb.postFlexion == 0.05)
  }

  it should "parse the diel YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.fish.verticalMigrationDielProbabilities.verticalMigrationDielProbability.size == 4)
    val dielProb = config.fish.verticalMigrationDielProbabilities.verticalMigrationDielProbability(0)

    assert(dielProb.depthStart == 0)
    assert(dielProb.depthFinish == 25)
    assert(dielProb.day == 0.1)
    assert(dielProb.night == 0.3)
  }
}

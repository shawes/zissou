package io.config

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.yaml

import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar

class ConfigurationTest extends flatspec.AnyFlatSpec with MockitoSugar {

  private val configYAML = """
      settings:
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
      larva:
        species: fish
        isMortal: true
        mortalityRate: 0.26
        ontogeny:
         hatching: 0
         preflexion: 2
         flexion: 5
         postflexion: 8
        swimming:
         strategy: one
         ability: directed
         criticalSwimmingSpeed: 0.463
         inSituSwimmingPotential: 0.25
         endurance: 0.5
         reynoldsEffect: false
         ageMaxSpeedReached: 1
         hatchSwimmingSpeed: 1
        pelagicLarvalDuration:
         mean: 18.3
         stdev: 1.5
         distribution: Normal
         pldType: Fixed
         nonSettlementPeriod: 5
        dielProbabilities:
         depths: [25,50,75,100]
         day: [0.1, 0.3, 0.4, 0.2]
         night: [0.3, 0.3, 0.3, 0.1]
        ovmProbabilities:
         implementation: Stage
         depths: [ 5, 50, 100 ]
         hatching: [ 0, 0, 0 ]
         preflexion: [ 0.4, 0.5, 0.1 ]
         flexion: [ 0.35, 0.5, 0.15 ]
         postflexion: [ 0.05, 0.85, 0.1 ]
      flow:
        netcdfFilePath: test1
        period:
         start: 2010-07-01
         end: 2011-08-31
        timeStep:
         unit: Hour
         duration: 2
        includeVerticalVelocity: true
      habitat:
        shapeFilePath: test2
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
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(config.isInstanceOf[Configuration])
  }

  it should "parse the settings YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(config.settings.randomSeed == 1234)
  }

  it should "parse the spawning YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(
      config.spawn.spawningLocation.size == 2,
      "Not correct number of spawning locations"
    )
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
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(config.turbulence.horizontalDiffusionCoefficient == 300)
    assert(config.turbulence.verticalDiffusionCoefficient == 15)
    assert(config.turbulence.applyTurbulence)
    assert(config.turbulence.interval == 1)
  }

  it should "parse the output YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(!config.output.includeLarvaeHistory)
    assert(config.output.saveOutputFilePath == "test3")
    assert(config.output.percentage == 5)
    assert(config.output.prefix == "test4")
    assert(config.output.logLevel == "info")
    assert(config.output.logFile == "test.log")
  }

  it should "parse the habitat YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(config.habitat.shapeFilePath == "test2")
    assert(config.habitat.buffer.settlement == 10)
    assert(config.habitat.buffer.olfactory == 10)
  }

  it should "parse the flow YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(config.flow.includeVerticalVelocity)
    assert(config.flow.netcdfFilePath == "test1")
    assert(config.flow.period.start == "2010-07-01")
    assert(config.flow.period.end == "2011-08-31")
    assert(config.flow.timeStep.unit == "Hour")
    assert(config.flow.timeStep.duration == 2)
  }

  it should "parse the ovm YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(
      config.larva.ovmProbabilities.get.implementation == "Stage"
    )
    assert(
      config.larva.ovmProbabilities.get.depths.size == 3
    )
  }

  it should "parse the diel YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
    assert(
      config.larva.dielProbabilities.get.depths.size == 4
    )
    assert(
      config.larva.dielProbabilities.get.day.size == 4
    )
    assert(
      config.larva.dielProbabilities.get.night.size == 4
    )

  }

  it should "parse the swimming YAML" in {
    val json = yaml.parser.parse("""     
      strategy: one
      ability: directed
      criticalSwimmingSpeed: 0.463
      inSituSwimmingPotential: 0.25
      endurance: 0.5
      reynoldsEffect: false
      ageMaxSpeedReached: 1
      hatchSwimmingSpeed: 1
  """)
    val config = json
      .leftMap(err => err: Error)
      .flatMap(_.as[SwimmingConfig])
      .valueOr(throw _)
    assert(config.ability.get == "directed")
  }
}

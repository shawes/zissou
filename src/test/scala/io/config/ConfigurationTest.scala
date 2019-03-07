package io.config

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.yaml

import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class ConfigurationTest extends FlatSpec with MockitoSugar {

  private val configYAML = """
      inputFiles:
        flowFilePath: test1
        habitatFilePath: test2
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

  "The configuration case class" should "parse the YAML file without error" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
  }

  it should "parse the input files YAML" in {
    val json = yaml.parser.parse(configYAML)
    val config = json.leftMap(err => err: Error).flatMap(_.as[Configuration]).valueOr(throw _)
    assert(config.inputFiles.flowFilePath == "test1")
    assert(config.inputFiles.habitatFilePath == "test2")
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

  //
  // it should "parse the flow node" in {
  //   val configXml = "<simulationVariables><flow><period><start>1996-12-01T00:00:00</start><end>1997-07-30T00:00:00</end></period><timeStep><unit>Hour</unit><duration>2</duration></timeStep><depth><average>true</average><averageOverAllDepths>true</averageOverAllDepths><maximumDepthForAverage>10</maximumDepthForAverage></depth></flow></simulationVariables>"
  //   val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
  //   assert(config.flow.period.start == "1996-12-01T00:00:00")
  //   assert(config.flow.period.end == "1997-07-30T00:00:00")
  //   assert(config.flow.timeStep.duration == 2)
  //   assert(config.flow.timeStep.unit == "Hour", "TimeStep unit was not parsed")
  //   assert(config.flow.depth.average)
  //   assert(config.flow.depth.averageOverAllDepths)
  //   assert(config.flow.depth.maximumDepthForAverage == 10)
  // }
  //
  // it should "parse the habitat node" in {
  //   val configXml = "<simulationVariables><habitat><buffer><isBuffered>true</isBuffered><settlement>25</settlement><olfactory>41</olfactory></buffer></habitat></simulationVariables>"
  //   val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
  //   assert(config.habitat.buffer.settlement == 25)
  //   assert(config.habitat.buffer.olfactory == 41)
  //   assert(config.habitat.buffer.isBuffered)
  // }
  //
  // it should "parse the output node" in {
  //   val configXml = "<simulationVariables><output><includeLarvaeHistory>true</includeLarvaeHistory><saveOutputFilePath>path</saveOutputFilePath><percentage>25</percentage><logLevel>verbose</logLevel></output></simulationVariables>"
  //   val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
  //   assert(config.output.includeLarvaeHistory)
  //   assert(config.output.logLevel == "verbose")
  //   assert(config.output.percentage == 25)
  //   assert(config.output.saveOutputFilePath == "path")
  // }
  //
  // it should "parse the fish node" in {
  //   val configXml = "<simulationVariables><fish> <ontogeny> <flexion>518400</flexion> <preFlexion>172800</preFlexion> <postFlexion>691200</postFlexion> </ontogeny> <swimming> <ability>Directed</ability> <criticalSwimmingSpeed>10.0</criticalSwimmingSpeed> <inSituSwimmingPotential>0.5</inSituSwimmingPotential> <endurance>0.4</endurance> <reynoldsEffect>true</reynoldsEffect> </swimming> <verticalMigrationDielProbabilities> <verticalMigrationDielProbability> <depthStart>0</depthStart> <depthFinish>50</depthFinish> <day>0.1</day> <night>0.9</night> </verticalMigrationDielProbability> <verticalMigrationDielProbability> <depthStart>50</depthStart> <depthFinish>100</depthFinish> <day>0.5</day> <night>0.5</night> </verticalMigrationDielProbability> </verticalMigrationDielProbabilities> <verticalMigrationOntogeneticProbabilities> <verticalMigrationOntogeneticProbability> <depthStart>3</depthStart> <depthFinish>10</depthFinish> <hatching>0.8</hatching> <preFlexion>0.05</preFlexion> <flexion>0.05</flexion> <postFlexion>0.05</postFlexion> </verticalMigrationOntogeneticProbability> <verticalMigrationOntogeneticProbability> <depthStart>10</depthStart> <depthFinish>50</depthFinish> <hatching>0.2</hatching> <preFlexion>0.55</preFlexion> <flexion>0.35</flexion> <postFlexion>0.55</postFlexion> </verticalMigrationOntogeneticProbability> </verticalMigrationOntogeneticProbabilities> <pelagicLarvalDuration> <mean>22</mean> <stdev>1.1</stdev> <distribution>Normal</distribution> </pelagicLarvalDuration> <isMortal>true</isMortal> <mortalityRate>0.26</mortalityRate> </fish></simulationVariables>"
  //   val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
  //   assert(config.fish.ontogeny.flexion == 518400)
  //   assert(config.fish.ontogeny.preFlexion == 172800)
  //   assert(config.fish.ontogeny.postFlexion == 691200)
  //   assert(config.fish.swimming.ability == "Directed")
  //   assert(config.fish.swimming.criticalSwimmingSpeed == 10.0)
  //   assert(config.fish.swimming.inSituSwimmingPotential == 0.5)
  //   assert(config.fish.swimming.endurance == 0.4)
  //   assert(config.fish.swimming.reynoldsEffect)
  //   assert(config.fish.verticalMigrationOntogeneticProbabilities.verticalMigrationOntogeneticProbability.size() == 2)
  //   val ontogeneticVerticalMigrationPattern = config.fish.verticalMigrationOntogeneticProbabilities.verticalMigrationOntogeneticProbability.get(0)
  //   assert(ontogeneticVerticalMigrationPattern.depthStart == 3)
  //   assert(ontogeneticVerticalMigrationPattern.depthFinish == 10)
  //   assert(ontogeneticVerticalMigrationPattern.flexion == 0.05)
  //   assert(ontogeneticVerticalMigrationPattern.preFlexion == 0.05)
  //   assert(ontogeneticVerticalMigrationPattern.postFlexion == 0.05)
  //   assert(ontogeneticVerticalMigrationPattern.hatching == 0.8)
  //   assert(config.fish.verticalMigrationDielProbabilities.verticalMigrationDielProbability.size() == 2)
  //   val dielVerticalMigrationPattern = config.fish.verticalMigrationDielProbabilities.verticalMigrationDielProbability.get(1)
  //   assert(dielVerticalMigrationPattern.depthStart == 50)
  //   assert(dielVerticalMigrationPattern.depthFinish == 100)
  //   assert(dielVerticalMigrationPattern.day == 0.5)
  //   assert(dielVerticalMigrationPattern.night == 0.5)
  //   assert(config.fish.pelagicLarvalDuration.distribution == "Normal")
  //   assert(config.fish.pelagicLarvalDuration.mean == 22)
  //   assert(config.fish.pelagicLarvalDuration.stdev == 1.1)
  //   assert(config.fish.isMortal)
  //   assert(config.fish.mortalityRate == 0.26)
  // }

}

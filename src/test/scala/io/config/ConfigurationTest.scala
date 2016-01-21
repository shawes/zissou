package io.config

import java.io.StringReader
import java.util.{ArrayList => JArrayList}
import javax.xml.bind.JAXBContext

import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar

class ConfigurationTest extends FlatSpec with MockitoSugar {

  val context = JAXBContext.newInstance(classOf[Configuration])

  "The configuration case class" should "parse the input files xml node" in {
    val configXml = "<simulationVariables><inputFiles><flowFilePath>flow</flowFilePath><habitatFilePath>habitat</habitatFilePath></inputFiles></simulationVariables>"
    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.inputFiles.flowFilePath == "flow")
    assert(config.inputFiles.habitatFilePath == "habitat")
  }

  it should "parse the spawn xml node" in {
    val configXml = "<simulationVariables><spawn><spawningLocation><name>loc1</name><patchNumber>186</patchNumber><site><longitude>153.086</longitude><latitude>-24.402</latitude><depth>5</depth><flowId>1</flowId></site><numberOfLarvae>10000</numberOfLarvae><releasePeriod><start>1996-12-01T00:00:00</start><end>1997-05-31T00:00:00</end></releasePeriod><interval>5</interval></spawningLocation><spawningLocation><name>loc2</name><patchNumber>186</patchNumber><site><longitude>153.086</longitude><latitude>-24.402</latitude><depth>5</depth><flowId>0</flowId></site><numberOfLarvae>10000</numberOfLarvae><releasePeriod><start>1996-12-01T00:00:00</start><end>1997-05-31T00:00:00</end></releasePeriod><interval>5</interval></spawningLocation></spawn></simulationVariables>"


    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.spawn.spawningLocation.size == 2, "Not correct number of spawning locations")
    val actual = config.spawn.spawningLocation.get(0)
    assert(actual.name == "loc1")
    assert(actual.numberOfLarvae == 10000)
    assert(actual.patchNumber == 186)
    assert(actual.interval == 5)
    assert(actual.site.latitude == -24.402)
    assert(actual.site.longitude == 153.086)
    assert(actual.site.flowId == 1)
    assert(actual.site.depth == 5)
    assert(actual.releasePeriod.start == "1996-12-01T00:00:00")
    assert(actual.releasePeriod.end == "1997-05-31T00:00:00")
  }

  /*  it should "generate the input files xml node" in {
      val configXml = "<simulationVariables><inputFiles><flowFilePath>flow</flowFilePath><habitatFilePath>habitat</habitatFilePath></inputFiles></simulationVariables>"

      val spawningLocations = new JArrayList[SpawningLocationConfig]
      //spawningLocations.add(new SpawningLocationConfig("bob"))
      //spawningLocations.add(new SpawningLocationConfig("sally"))
      context.createMarshaller.marshal(Configuration(
        InputFilesConfig("flow", "habitat"),
        SpawnConfig(spawningLocations),
        TurbulenceConfig(0, 0, false, 0),
        FlowConfig(PeriodConfig("", ""), TimeStepConfig("", "", 0), DepthConfig(false, false, 0)),
      HabitatConfig(BufferConfig(false, 0, "")),
      OutputFilesConfig(false,"","",0,"")),
        System.out)
    }*/

  it should "parse the turbulence node" in {

    val configXml = "<simulationVariables><turbulence><horizontalDiffusionCoefficient>8.3</horizontalDiffusionCoefficient><verticalDiffusionCoefficient>0.2</verticalDiffusionCoefficient><applyTurbulence>true</applyTurbulence><interval>1</interval></turbulence></simulationVariables>"
    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.turbulence.horizontalDiffusionCoefficient == 8.3)
    assert(config.turbulence.verticalDiffusionCoefficient == 0.2)
    assert(config.turbulence.applyTurbulence)
    assert(config.turbulence.interval == 1)

  }

  it should "parse the flow node" in {
    val configXml = "<simulationVariables><flow><period><start>1996-12-01T00:00:00</start><end>1997-07-30T00:00:00</end></period><timeStep><unit>Hour</unit><duration>2</duration></timeStep><depth><average>true</average><averageOverAllDepths>true</averageOverAllDepths><maximumDepthForAverage>10</maximumDepthForAverage></depth></flow></simulationVariables>"
    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.flow.period.start == "1996-12-01T00:00:00")
    assert(config.flow.period.end == "1997-07-30T00:00:00")
    assert(config.flow.timeStep.duration == 2)
    assert(config.flow.timeStep.unit == "Hour", "TimeStep unit was not parsed")
    assert(config.flow.depth.average)
    assert(config.flow.depth.averageOverAllDepths)
    assert(config.flow.depth.maximumDepthForAverage == 10)
  }

  it should "parse the habitat node" in {
    val configXml = "<simulationVariables><habitat><buffer><isBuffered>true</isBuffered><bufferSize>25</bufferSize><filePath>path</filePath></buffer></habitat></simulationVariables>"
    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.habitat.buffer.bufferSize == 25)
    assert(config.habitat.buffer.isBuffered)
    assert(config.habitat.buffer.filePath == "path")
  }

  it should "parse the output node" in {
    val configXml = "<simulationVariables><output><includeLarvaeHistory>true</includeLarvaeHistory><shape>line</shape><saveOutputFilePath>path</saveOutputFilePath><percentage>25</percentage><logLevel>verbose</logLevel></output></simulationVariables>"
    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.output.shape == "line")
    assert(config.output.includeLarvaeHistory)
    assert(config.output.logLevel == "verbose")
    assert(config.output.percentage == 25)
    assert(config.output.saveOutputFilePath == "path")
  }

  it should "parse the fish node" in {
    val configXml = "<simulationVariables><fish><swimmingAbility>Passive</swimmingAbility><ontogeny><flexion>518400</flexion><preFlexion>172800</preFlexion><postFlexion>691200</postFlexion></ontogeny><meanSwimmingSpeed>0.102</meanSwimmingSpeed><standDeviationSwimmingSpeed>0.019</standDeviationSwimmingSpeed><verticalMigrationPattern>Ontogenetic</verticalMigrationPattern><verticalMigrationProbabilities><verticalMigrationProbability><depth>3</depth><hatching>0.8</hatching><preFlexion>0.05</preFlexion><flexion>0.05</flexion><postFlexion>0.05</postFlexion></verticalMigrationProbability><verticalMigrationProbability><depth>10</depth><hatching>0.2</hatching><preFlexion>0.55</preFlexion><flexion>0.35</flexion><postFlexion>0.55</postFlexion></verticalMigrationProbability></verticalMigrationProbabilities><pelagicLarvalDuration><mean>22</mean><stdev>1.1</stdev><distribution>Normal</distribution></pelagicLarvalDuration><isMortal>true</isMortal><mortalityRate>0.26</mortalityRate><nonSettlementPeriod>85</nonSettlementPeriod></fish></simulationVariables>"
    val config = context.createUnmarshaller().unmarshal(new StringReader(configXml)).asInstanceOf[Configuration]
    assert(config.fish.swimmingAbility == "Passive")
    assert(config.fish.ontogeny.flexion == 518400)
    assert(config.fish.ontogeny.preFlexion == 172800)
    assert(config.fish.ontogeny.postFlexion == 691200)
    assert(config.fish.meanSwimmingSpeed == 0.102)
    assert(config.fish.standDeviationSwimmingSpeed == 0.019)
    assert(config.fish.verticalMigrationPattern == "Ontogenetic")
    assert(config.fish.verticalMigrationProbabilities.verticalMigrationProbability.size() == 2)
    val verticalMigrationPattern = config.fish.verticalMigrationProbabilities.verticalMigrationProbability.get(0)
    assert(verticalMigrationPattern.depth == 3)
    assert(verticalMigrationPattern.flexion == 0.05)
    assert(verticalMigrationPattern.preFlexion == 0.05)
    assert(verticalMigrationPattern.postFlexion == 0.05)
    assert(verticalMigrationPattern.hatching == 0.8)
    assert(config.fish.pelagicLarvalDuration.distribution == "Normal")
    assert(config.fish.pelagicLarvalDuration.mean == 22)
    assert(config.fish.pelagicLarvalDuration.stdev == 1.1)
    assert(config.fish.isMortal)
    assert(config.fish.mortalityRate == 0.26)
    assert(config.fish.nonSettlementPeriod == 85)
  }

}

package io.config

import java.util.{ArrayList => JArrayList, Arrays => JArrays, List => JList}
import javax.xml.bind.annotation._

@XmlRootElement(name = "simulationVariables")
@XmlAccessorType(XmlAccessType.FIELD)
case class Configuration(
                          @XmlElementWrapper inputFiles: InputFilesConfig,
                          @XmlElementWrapper spawn: SpawnConfig,
                          @XmlElementWrapper turbulence: TurbulenceConfig,
                          @XmlElementWrapper fish: FishConfig,
                          @XmlElementWrapper flow: FlowConfig,
                          @XmlElementWrapper habitat: HabitatConfig,
                          @XmlElementWrapper output: OutputFilesConfig) {
  def this() = this(InputFilesConfig("", ""),
    SpawnConfig(new JArrayList[SpawningLocationConfig]),
    TurbulenceConfig(0, 0, applyTurbulence = false, 0),
    FishConfig(OntogenyConfig(0, 0, 0), SwimmingConfig("", 0,0,0,false),
      VerticalMigrationOntogeneticConfig(new JArrayList[VerticalMigrationOntogeneticProbabilityConfig]),
      VerticalMigrationDielConfig(new JArrayList[VerticalMigrationDielProbabilityConfig]),
      PelagicLarvalDurationConfig(0, 0, "", "", 0), isMortal = false, 0),
    FlowConfig(PeriodConfig("", ""),
      TimeStepConfig("", 0),
      DepthConfig(average = false, averageOverAllDepths = false, 0)),
    HabitatConfig(BufferConfig(isBuffered = false, 0, 0)),
    OutputFilesConfig(includeLarvaeHistory = false, "", "", 0, "",""))
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class InputFilesConfig(
                             flowFilePath: String,
                             habitatFilePath: String) {
  private def this() = this("", "")
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class SpawningLocationConfig(
                                   name: String,
                                   patchNumber: Int,
                                   site: SiteConfig,
                                   numberOfLarvae: Int,
                                   releasePeriod: ReleasePeriodConfig,
                                   interval: Int) {
  private def this() = this("", 0, SiteConfig(0, 0, 0, 0), 0, null, 0)
}

@XmlRootElement(name = "spawn")
@XmlAccessorType(XmlAccessType.FIELD)
case class SpawnConfig(
                        @XmlElements(
                          value = Array(new XmlElement(name = "spawningLocation"))
                        )
                        spawningLocation: java.util.List[SpawningLocationConfig]
                        ) {
  private def this() = this(new JArrayList[SpawningLocationConfig])
}


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class SiteConfig(
                       longitude: Double,
                       latitude: Double,
                       depth: Double,
                       flowId: Int) {
  private def this() = this(0, 0, 0, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class ReleasePeriodConfig(start: String, end: String) {
  private def this() = this("", "")
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class TurbulenceConfig(
                             horizontalDiffusionCoefficient: Double,
                             verticalDiffusionCoefficient: Double,
                             applyTurbulence: Boolean,
                             interval: Int) {
  private def this() = this(0, 0, false, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class FlowConfig(
                       @XmlElementWrapper period: PeriodConfig,
                       @XmlElementWrapper timeStep: TimeStepConfig,
                       @XmlElementWrapper depth: DepthConfig) {
  private def this() = this(PeriodConfig("", ""), TimeStepConfig("", 0), DepthConfig(average = false, averageOverAllDepths = false, 0))
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class PeriodConfig(start: String, end: String) {
  private def this() = this("", "")
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class TimeStepConfig(unit: String, duration: Int) {
  private def this() = this("", 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class DepthConfig(average: Boolean, averageOverAllDepths: Boolean, maximumDepthForAverage: Int) {
  private def this() = this(false, false, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class HabitatConfig(@XmlElementWrapper buffer: BufferConfig) {
  private def this() = this(BufferConfig(isBuffered = false, 0, 0))
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class BufferConfig(isBuffered: Boolean, settlement: Double, olfactory : Double) {
  private def this() = this(false, 0, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class OutputFilesConfig(includeLarvaeHistory: Boolean,
                             shape: String,
                             saveOutputFilePath: String,
                             percentage: Int,
                             logLevel: String,
                             logFile: String) {
  private def this() = this(false, "", "", 0, "", "")
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class FishConfig(ontogeny: OntogenyConfig,
                      swimming: SwimmingConfig,
                      verticalMigrationOntogeneticProbabilities: VerticalMigrationOntogeneticConfig,
                      verticalMigrationDielProbabilities: VerticalMigrationDielConfig,
                      pelagicLarvalDuration: PelagicLarvalDurationConfig,
                      isMortal: Boolean,
                      mortalityRate: Double) {
  private def this() = this(OntogenyConfig(0, 0, 0), SwimmingConfig("",0,0,0,false), VerticalMigrationOntogeneticConfig(new JArrayList[VerticalMigrationOntogeneticProbabilityConfig]),VerticalMigrationDielConfig(new JArrayList[VerticalMigrationDielProbabilityConfig]), PelagicLarvalDurationConfig(0, 0, "", "", 0), false, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class OntogenyConfig(preFlexion: Int, flexion: Int, postFlexion: Int) {
  private def this() = this(0, 0, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class SwimmingConfig(ability: String, criticalSwimmingSpeed: Double, inSituSwimmingPotential: Double, endurance: Double, reynoldsEffect: Boolean) {
  private def this() = this(null, 0, 0, 0, false)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class PelagicLarvalDurationConfig(mean: Double, stdev: Double, distribution: String, pldType : String, nonSettlementPeriod: Double) {
  private def this() = this(0, 0, "","", 0)
}

@XmlRootElement(name = "verticalMigrationOntogeneticProbabilities")
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationOntogeneticConfig(
                                    @XmlElements(
                                      value = Array(new XmlElement(name = "verticalMigrationOntogeneticProbability"))
                                    )
                                    verticalMigrationOntogeneticProbability: java.util.List[VerticalMigrationOntogeneticProbabilityConfig]
                                    ) {
  private def this() = this(new JArrayList[VerticalMigrationOntogeneticProbabilityConfig])
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationOntogeneticProbabilityConfig(depthStart: Int,
                                              depthFinish: Int,
                                              hatching: Double,
                                              preFlexion: Double,
                                              flexion: Double,
                                              postFlexion: Double) {
  private def this() = this(0, 0, 0, 0, 0, 0)
}

@XmlRootElement(name = "verticalMigrationDielProbabilities")
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationDielConfig(
                                    @XmlElements(
                                      value = Array(new XmlElement(name = "verticalMigrationDielProbability"))
                                    )
                                    verticalMigrationDielProbability: java.util.List[VerticalMigrationDielProbabilityConfig]
                                    ) {
  private def this() = this(new JArrayList[VerticalMigrationDielProbabilityConfig])
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationDielProbabilityConfig(depthStart: Int,
                                              depthFinish: Int,
                                              day : Double,
                                              night : Double) {
  private def this() = this(0, 0, 0, 0)
}

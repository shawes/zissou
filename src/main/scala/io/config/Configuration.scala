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
    FishConfig(OntogenyConfig(0, 0, 0), "", 0, 0, "",
      VerticalMigrationConfig(new JArrayList[VerticalMigrationProbabilityConfig]),
      PelagicLarvalDurationConfig(0, 0, ""), isMortal = false, 0, 0),
    FlowConfig(PeriodConfig("", ""),
      TimeStepConfig("", 0),
      DepthConfig(average = false, averageOverAllDepths = false, 0)),
    HabitatConfig(BufferConfig(isBuffered = false, 0, "")),
    OutputFilesConfig(includeLarvaeHistory = false, "", "", 0, ""))
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
  private def this() = this(BufferConfig(isBuffered = false, 0, ""))
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class BufferConfig(isBuffered: Boolean, bufferSize: Int, filePath: String) {
  private def this() = this(false, 0, "")
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class OutputFilesConfig(includeLarvaeHistory: Boolean,
                             shape: String,
                             saveOutputFilePath: String,
                             percentage: Int,
                             logLevel: String) {
  private def this() = this(false, "", "", 0, "")
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class FishConfig(ontogeny: OntogenyConfig,
                      swimmingAbility: String,
                      meanSwimmingSpeed: Double,
                      standDeviationSwimmingSpeed: Double,
                      verticalMigrationPattern: String,
                      verticalMigrationProbabilities: VerticalMigrationConfig,
                      pelagicLarvalDuration: PelagicLarvalDurationConfig,
                      isMortal: Boolean,
                      mortalityRate: Double,
                      nonSettlementPeriod: Int) {
  private def this() = this(OntogenyConfig(0, 0, 0), "", 0, 0, "", VerticalMigrationConfig(new JArrayList[VerticalMigrationProbabilityConfig]), PelagicLarvalDurationConfig(0, 0, ""), false, 0, 0)

}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class OntogenyConfig(preFlexion: Int, flexion: Int, postFlexion: Int) {
  private def this() = this(0, 0, 0)
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class PelagicLarvalDurationConfig(mean: Double, stdev: Double, distribution: String) {
  private def this() = this(0, 0, "")
}

@XmlRootElement(name = "verticalMigrationProbabilities")
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationConfig(
                                    @XmlElements(
                                      value = Array(new XmlElement(name = "verticalMigrationProbability"))
                                    )
                                    verticalMigrationProbability: java.util.List[VerticalMigrationProbabilityConfig]
                                    ) {
  private def this() = this(new JArrayList[VerticalMigrationProbabilityConfig])
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationProbabilityConfig(depthStart: Int,
                                              depthFinish: Int,
                                              hatching: Double,
                                              preFlexion: Double,
                                              flexion: Double,
                                              postFlexion: Double) {
  private def this() = this(0, 0, 0, 0, 0, 0)
}

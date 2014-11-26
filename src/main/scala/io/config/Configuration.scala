package io.config

import javax.xml.bind.annotation._
import java.util.{List => JList}
import java.util.{ArrayList => JArrayList}
import java.util.{Arrays => JArrays}

@XmlRootElement(name = "simulationVariables")
@XmlAccessorType(XmlAccessType.FIELD)
case class Configuration(

                          //@XmlElement(required = true, name="Fish") val fish : FishConfig,
                          @XmlElementWrapper inputFiles: InputFilesConfig,
                          //@XmlElementWrapper(name="spawn") @XmlElement spawningLocation: java.util.List[SpawningLocationConfig],
                          @XmlElementWrapper spawn: SpawnConfig,
                          @XmlElementWrapper turbulence: TurbulenceConfig,
                          @XmlElementWrapper fish: FishConfig,
                          @XmlElementWrapper flow: FlowConfig,
                          @XmlElementWrapper habitat: HabitatConfig,
                          @XmlElementWrapper output: OutputFilesConfig) {
  def this() = this(InputFilesConfig("", ""),
    SpawnConfig(new JArrayList[SpawningLocationConfig]),
    TurbulenceConfig(0, 0, false, 0),
    FishConfig(OntogenyConfig(0, 0, 0), "", 0, 0, "", VerticalMigrationConfig(new JArrayList[VerticalMigrationProbabilityConfig]), PelagicLarvalDurationConfig(0, 0, ""), false, 0, 0),
    FlowConfig(PeriodConfig("", ""), TimeStepConfig("", 0), DepthConfig(false, false, 0)),
    HabitatConfig(BufferConfig(false, 0, "")),
    OutputFilesConfig(false, "", "", 0, ""))
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class InputFilesConfig(
                             flowFilePath: String,
                             habitatFilePath: String) {
  private def this() = this("", "")
}

@XmlRootElement(name = "spawn")
@XmlAccessorType(XmlAccessType.FIELD)
case class SpawnConfig(
                        @XmlElements(
                          value = Array(new XmlElement(name = "spawningLocation", `type` = classOf[SpawningLocationConfig]))
                        )
                        spawningLocation: JList[SpawningLocationConfig]
                        ) {
  private def this() = this(new JArrayList[SpawningLocationConfig])
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class SpawningLocationConfig(
                                   name: String,
                                   patchNumber: Int,
                                   site: SiteConfig,
                                   numberOfLarvae: Int,
                                   releasePeriod: ReleasePeriodConfig, interval: Int) {
  private def this() = this("", 0, null, 0, null, 0)
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
  private def this() = this(PeriodConfig("", ""), TimeStepConfig("", 0), DepthConfig(false, false, 0))
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
                                      value = Array(new XmlElement(name = "verticalMigrationProbability", `type` = classOf[VerticalMigrationProbabilityConfig]))
                                    )
                                    verticalMigrationProbability: JList[VerticalMigrationProbabilityConfig]
                                    ) {
  private def this() = this(new JArrayList[VerticalMigrationProbabilityConfig])
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class VerticalMigrationProbabilityConfig(depth: Int,
                                              hatching: Double,
                                              preFlexion: Double,
                                              flexion: Double,
                                              postFlexion: Double) {
  private def this() = this(0, 0, 0, 0, 0)
}
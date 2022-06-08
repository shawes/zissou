package locals

sealed trait HabitatType
case object Beach extends HabitatType
case object Land extends HabitatType
case object Mangrove extends HabitatType
case object Reef extends HabitatType
case object RockyIntertidal extends HabitatType
case object Saltmarsh extends HabitatType
case object Seagrass extends HabitatType
case object SubtidalSand extends HabitatType
case object Ocean extends HabitatType
case object Other extends HabitatType

enum Bob:
    case Frank, Steve

sealed trait PelagicLarvalDurationType
case object Random extends PelagicLarvalDurationType
case object Fixed extends PelagicLarvalDurationType

sealed trait OntogeneticMigrationStrategy
case object OntogeneticStageMigration extends OntogeneticMigrationStrategy
case object DailyMigration extends OntogeneticMigrationStrategy
case object TimeStepMigration extends OntogeneticMigrationStrategy

sealed trait DielVerticalMigrationType
case object DayMigration extends DielVerticalMigrationType
case object NightMigration extends DielVerticalMigrationType

sealed trait LarvaType
case object Fish extends LarvaType

// enum LarvaType:
//     case Fish, Invertebrate

sealed trait PelagicLarvaeState
case object Pelagic extends PelagicLarvaeState
case object Dead extends PelagicLarvaeState
case object Settled extends PelagicLarvaeState

sealed trait OntogeneticState
case object Hatching extends OntogeneticState
case object Preflexion extends OntogeneticState
case object Flexion extends OntogeneticState
case object Postflexion extends OntogeneticState

sealed trait InterpolationTime
case object InterpolateToday extends InterpolationTime
case object InterpolateTomorrow extends InterpolationTime

sealed trait InterpolationType
case object Bilinear extends InterpolationType
case object Bicubic extends InterpolationType
case object Trilinear extends InterpolationType
case object Tricubic extends InterpolationType

sealed trait QuadrantLocation
case object TopLeft extends QuadrantLocation
case object TopRight extends QuadrantLocation
case object BottomLeft extends QuadrantLocation
case object BottomRight extends QuadrantLocation

sealed trait HorizontalSwimmingStrategy
case object StrategyOne extends HorizontalSwimmingStrategy
case object StrategyTwo extends HorizontalSwimmingStrategy
case object StrategyThree extends HorizontalSwimmingStrategy

sealed trait SwimmingAbility
case object Passive extends SwimmingAbility
case object Directed extends SwimmingAbility
case object Undirected extends SwimmingAbility

sealed trait Distribution
case object Normal extends Distribution
case object Skewed extends Distribution
case object LogNormal extends Distribution
case object Decay extends Distribution

sealed trait SunDirection
case object Rising extends SunDirection
case object Setting extends SunDirection

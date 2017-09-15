package locals

// Converted to a sealed class
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

sealed trait PelagicLarvalDurationType
case object Random extends PelagicLarvalDurationType
case object Fixed extends PelagicLarvalDurationType

sealed trait OntogeneticVerticalMigrationImpl
case object StageMigration extends OntogeneticVerticalMigrationImpl
case object DailyMigration extends OntogeneticVerticalMigrationImpl
case object TimestepMigration extends OntogeneticVerticalMigrationImpl



object PelagicLarvaeState extends Enumeration {
  type PelagicLarvaeState = Value
  val Pelagic, Dead, Settled = Value
}

object Direction extends Enumeration {
  type Direction = Value
  val North, East, South, West = Value
}

object TimeStepType extends Enumeration {
  type TimeStepType = Value
  val Second, Hour, Day = Value
}

object OntogeneticVerticalMigrationType extends Enumeration {
  type OntogeneticVerticalMigrationType = Value
  val Random, Restricted, StageBased = Value
}

object DielVerticalMigrationType extends Enumeration {
  type DielVerticalMigrationType = Value
  val Day, Night = Value
}

object DistributionType extends Enumeration {
  type DistributionType = Value
  val Normal, Skewed = Value
}

object VerticalMigrationPattern extends Enumeration {
  type VerticalMigrationPattern = Value
  val None, Diel, Ontogenetic, Both = Value
}

object SwimmingAbility extends Enumeration {
  type SwimmingAbility = Value
  val Passive, Directed, Undirected = Value
}

object LoggingLevel extends Enumeration {
  type LoggingLevel = Value
  val None, Concise, Verbose = Value
}

object OntogenyState extends Enumeration {
  type OntogenyState = Value
  val Hatching, Preflexion, Flexion, Postflexion = Value
}

object LarvaType extends Enumeration {
  type LarvaType = Value
  val Fish = Value
}

object QuadrantType extends Enumeration {
  type QuadrantType = Value
  val TopLeft, TopRight, BottomLeft, BottomRight = Value
}

object InterpolationType extends Enumeration {
  type InterpolationType = Value
  val Bilinear, TriLinear, Bicubic, Tricubic = Value
}

object Day extends Enumeration {
  type Day = Value
  val Today, Tomorrow = Value
}

object Sun extends Enumeration {
  type Sun = Value
  val Rising, Setting = Value
}

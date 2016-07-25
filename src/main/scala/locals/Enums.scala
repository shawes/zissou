package locals

object HabitatType extends Enumeration {
  type HabitatType = Value
  val Beach, Land, Mangrove, Reef, RockyIntertidal, Saltmarsh, Seagrass, SubtidalSand, Ocean, Other = Value
}

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
  val Shallow, Deep = Value
}

object DistributionType extends Enumeration {
  type DistributionType = Value
  val Normal, Skewed = Value
}

object VerticalMigrationPattern extends Enumeration {
  type VerticalMigrationPattern = Value
  val None, Dial, Ontogenetic = Value
}

object SwimmingAbility extends Enumeration {
  type SwimmingAbility = Value
  val Passive, Random, Directed, Undirected = Value
}

object ShapeFileType extends Enumeration {
  type ShapeFileType = Value
  val Point, Line = Value
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
  val ReefFish = Value
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



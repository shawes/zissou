package locals

object Enums:
  enum HabitatType:
    case Land, Reef, RockyIntertidal, Ocean, Other
  enum PelagicLarvalDurationType:
    case Random, Fixed
  enum OntogeneticMigrationType:
    case Stage, Daily, TimeStep
  enum DielVerticalMigrationType:
    case Day, Night
  enum LarvaType:
    case Fish, Invertebrate
  enum PelagicLarvaeState:
    case Pelagic, Dead, Settled
  enum OntogeneticState:
    case Hatching, Preflexion, Flexion, Postflexion
  enum InterpolationTime:
    case Today, Tomorrow
  enum InterpolationType:
    case Bilinear, Bicubic, Trilinear, Tricubic
  enum QuadrantLocation:
    case TopLeft, TopRight, BottomLeft, BottomRight
  enum SwimmingStrategy:
    case One, Two, Three
  enum Swims:
    case Passive, Directed, Undirected
  enum Distribution:
    case Normal, Skewed, LogNormal, Decay
  enum SunDirection:
    case Rising, Setting

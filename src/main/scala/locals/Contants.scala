package locals

class Constants {}

object Constants {

  val SecondsInDay = 3600
  val FlowPolygonVertices = 4
  val MaxLatitudeShift = 0.015
  val MaxLongitudeShift = 0.015
  val LarvaeCapacityAtSite = 10000
  val NoClosestReefFound = -1

  object ShapeAttribute {
    val Geometry = (0, "the_geom")
    val Habitat = (1, "HABITAT")
    val State = (2, "STATE")
    val Group = (3, "GROUPID")
    val Patch = (4, "PATCH_NUM")
    val XCoordinate = (5, "X_COORD")
    val YCoordinate = (6, "Y_COORD")
    val Area = (7, "AREA")
    val Perimeter = (8, "PERIMETER")
  }


}

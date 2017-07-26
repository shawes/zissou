package locals

import org.joda.time.DateTime

object Constants {

  val SecondsInDay = 86400
  val DuskOrDawn = 2700 //Dusk or dawn roughly starts / ends 45 mins before or after sunset / sunrise
  val FlowPolygonVertices = 4
  val MaxLatitudeShift = 0.005
  val MaxLongitudeShift = 0.005
  val LarvaeCapacityAtSite = 10000
  //val NoClosestReefFound = -1
  val Ocean = -27
  val MinimumDate = new DateTime(1976,1,1,0,0)
  val EarthsRadius = 6378137.0

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

  object LightWeightException {
    val NoReefFoundException = -100
    val UndefinedVelocityException = -200
    val UndefinedCoordinateException = -300
    val CoordinateNotFoundException = -400
  }

  object Interpolation {
    val CubicPoints = 4
    val BicubicPoints = 16
    val TricubicPoints = 64
  }

  object NetcdfIndex {
    val X = 0
    val Y = 1
    val Z = 2
    val Time = 3
  }


}

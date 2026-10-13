package physical.flow

import com.github.nscala_time.time.Imports._
import maths.ContinuousRange
import physical.{Grid, TimeStep}

/** The part of the ocean model's grid that is read, in degrees: latitude south to north, longitude west to east. */
case class FlowRegion(south: Double, north: Double, west: Double, east: Double) {
  require(south < north, s"The south of the flow region ($south) must be less than its north ($north)")
  require(west < east, s"The west of the flow region ($west) must be less than its east ($east)")

  def contains(latitude: Double, longitude: Double): Boolean =
    latitude >= south && latitude <= north && longitude >= west && longitude <= east
}

object FlowRegion {
  /** The region Zissou has always read: the east coast of Australia and the Coral Sea. */
  val Default: FlowRegion = FlowRegion(-50.0, -10.0, 142.0, 180.0)
  /** The number of depth levels, from the surface, that Zissou has always read. */
  val DefaultDepthLevels = 15
}

class Flow(
    val netcdfFilePath: String,
    var period: Interval,
    var timeStep: TimeStep,
    val includeVerticalVelocity: Boolean,
    val region: FlowRegion = FlowRegion.Default,
    val depthLevels: Int = FlowRegion.DefaultDepthLevels,
    val filePrefix: String = "ocean",
    /** Finer models nested inside this one, tried in this order before it: list the finest first. */
    val nests: List[Flow] = List.empty
) {
  var dimensions = new Dimensions(
    new ContinuousRange(),
    new ContinuousRange(),
    new ContinuousRange(),
    new Grid()
  )
  // def this(peoperties: OceanData) = this(new Grid(), new ContinuousRange(), new ContinuousRange(), depth, null, new TimeStep())
}

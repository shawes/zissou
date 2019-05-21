package physical.flow

import com.github.nscala_time.time.Imports._
import maths.ContinuousRange
import physical.{Grid, TimeStep}

class Flow(
    val netcdfFilePath: String,
    var depth: Depth,
    var period: Interval,
    var timeStep: TimeStep,
    val includeVerticalVelocity: Boolean
) {
  var dimensions = new Dimensions(
    new ContinuousRange(),
    new ContinuousRange(),
    new ContinuousRange(),
    new Grid()
  )
  // def this(peoperties: OceanData) = this(new Grid(), new ContinuousRange(), new ContinuousRange(), depth, null, new TimeStep())
}

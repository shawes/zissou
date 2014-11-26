package physical.flow

import com.github.nscala_time.time.Imports._
import physical.{TimeStep, Grid}
import maths.ContinuousRange

class Flow(var grid: Grid,
           var latitudeRange: ContinuousRange,
           var longitudeRange: ContinuousRange,
           var depth: Depth,
           var period: Interval,
           var timeStep: TimeStep) {
  def this() = this(new Grid(), null, null, new Depth(), null, null)

  def this(depth: Depth) = this(new Grid(), null, null, depth, null, null)
}

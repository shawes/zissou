package physical.flow

import com.github.nscala_time.time.Imports._
import maths.ContinuousRange
import physical.{Grid, TimeStep}

class Flow(var grid: Grid,
           var latitudeRange: ContinuousRange,
           var longitudeRange: ContinuousRange,
           var depth: Depth,
           var period: Interval,
           var timeStep: TimeStep) {
  def this() = this(new Grid(), new ContinuousRange(), new ContinuousRange(), new Depth(), null, new TimeStep())

  def this(depth: Depth) = this(new Grid(), new ContinuousRange(), new ContinuousRange(), depth, null, new TimeStep())
}

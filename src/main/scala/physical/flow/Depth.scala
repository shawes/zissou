package physical.flow

import maths.ContinuousRange
import io.config.DepthConfig

class Depth(var average: Boolean,
            var averageOverAllDepths: Boolean,
            var maximumDepthForAverage: Int,
            var range: ContinuousRange) {


  def this(config: DepthConfig) = this(config.average, config.averageOverAllDepths, config.maximumDepthForAverage, new ContinuousRange(0, 0, false))

  def this() = this(false, false, 0, new ContinuousRange(0, 0, false))
}

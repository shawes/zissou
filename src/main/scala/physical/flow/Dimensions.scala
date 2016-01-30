package physical.flow

import maths.ContinuousRange
import physical.Grid

/**
  *
  * Created by Steven Hawes on 30/01/2016.
  */
class Dimensions(var latitudeBoundary: ContinuousRange,
                 var longitudeBoundary: ContinuousRange,
                 var depth: ContinuousRange,
                 var cellSize: Grid) {

}

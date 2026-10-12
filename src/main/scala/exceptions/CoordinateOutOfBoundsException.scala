package exceptions

import scala.util.control.NoStackTrace

/** A coordinate lies outside the horizontal extent of the flow grid. */
class CoordinateOutOfBoundsException(message: String)
    extends Exception(message)
    with NoStackTrace

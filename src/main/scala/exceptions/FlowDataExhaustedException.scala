package exceptions

import scala.util.control.NoStackTrace

/** The flow files hold no data for a day the simulation needs. */
class FlowDataExhaustedException(message: String)
    extends Exception(message)
    with NoStackTrace

package physical

import locals._

class TimeStep(val duration: Double, val timePeriod: String) {
  lazy val totalSeconds: Int = calculateTimeStepInSeconds

  private def calculateTimeStepInSeconds: Int = timePeriod match {
    case "second" => duration.toInt
    case "hour"   => (3600 * duration).toInt
    case "day"    => (86400 * duration).toInt
    case _        => throw new NoSuchFieldException()
  }

}

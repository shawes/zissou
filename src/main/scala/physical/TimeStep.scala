package physical

import locals.TimeStepType
import locals.TimeStepType.TimeStepType

class TimeStep(val duration: Double, val timeType: TimeStepType) {
  lazy val totalSeconds: Int = calculateTimeStepInSeconds

  def this() = this(0.0, TimeStepType.Second)

  private def calculateTimeStepInSeconds: Int = timeType match {
    case TimeStepType.Second => duration.toInt
    case TimeStepType.Hour => (3600 * duration).toInt
    case TimeStepType.Day => (86400 * duration).toInt
    case _ => throw new NoSuchFieldException()
  }

}

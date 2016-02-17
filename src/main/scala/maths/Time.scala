package maths

import locals.Constants

object Time {
  def convertDaysToSeconds(days: Double): Int = (days * Constants.SecondsInDay).toInt
}

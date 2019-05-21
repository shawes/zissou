package utilities

import locals.Constants

object Time {
  def convertDaysToSeconds(days: Int): Int = (days * Constants.SecondsInDay)
  def convertDaysToSeconds(days: Double): Int =
    (days * Constants.SecondsInDay).toInt
  def convertSecondsToDays(seconds: Int) =
    seconds / Constants.SecondsInDay.toDouble
}

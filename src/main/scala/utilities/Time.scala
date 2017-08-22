package utilities

import locals.Constants

object Time {
  def convertDaysToSeconds(days: Int): Int = (days * Constants.SecondsInDay)
  def convertDaysToSeconds(days: Double): Int = convertDaysToSeconds(days.toInt)
  def convertSecondsToDays(seconds: Int): Int = seconds / Constants.SecondsInDay
}

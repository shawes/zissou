package main

import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import physical.TimeStep

/**
  * Created by steve on 27/01/2016.
  */
class SimulationClock(interval: Interval, val step: TimeStep) {

  val start = interval.getStart
  val end = interval.getEnd
  val totalDays: Int = Days.daysBetween(start, end).getDays
  var now: DateTime = start

  def tick(): Unit = {
    now = now.plusSeconds(step.totalSeconds)
  }

  def stillTime: Boolean = now.isBefore(end)
  def isMidnight: Boolean = now.getHourOfDay == 0


}

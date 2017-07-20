package utilities

import org.joda.time.DateTime

class SimpleTimer() {

  //var start = DateTime.now
  private val Start = 0
  private val Stop = 1
  private val timer = Array.ofDim[DateTime](2)

  def start(): Unit = {
    timer(Start) = DateTime.now
  }

  def stop(): Unit = {
    timer(Stop) = DateTime.now
  }

  def result(): Int = {
    timer(Stop).getSecondOfDay - timer(Start).getSecondOfDay
  }

}

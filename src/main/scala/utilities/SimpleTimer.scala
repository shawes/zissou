package utilities

import com.github.nscala_time.time.Imports._

class SimpleTimer() {

  //var start = DateTime.now
  private val Start = 0
  private val Stop = 1
  private val timer = Array.ofDim[DateTime](2)
  timer(Start) = DateTime.now()

  def start(): Unit = {
    timer(Start) = DateTime.now()
  }

  def stop(): Int = {
    timer(Stop) = DateTime.now()
    timer(Stop).getSecondOfDay - timer(Start).getSecondOfDay
  }
}

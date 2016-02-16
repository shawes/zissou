package utilities

import org.joda.time.DateTime

class Timer() {

  var start = DateTime.now

  def stop(): Double = {
    DateTime.now.getSecondOfDay - start.getSecondOfDay
  }

  def reset(): Unit = {
    start = DateTime.now
  }

}

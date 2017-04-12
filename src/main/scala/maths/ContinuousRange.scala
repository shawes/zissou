package maths

class ContinuousRange(val start: Double, val end: Double, val isInclusive: Boolean) {
  def this() = this(0, 0, false)
  def contains(value: Double): Boolean = {
    if (isInclusive) {
      value >= start && value <= end
    } else {
      value > start && value < end
    }
  }
}

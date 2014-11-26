package maths

class ContinuousRange(val start: Double, val end: Double, inclusive: Boolean) {
  def contains(value: Double): Boolean = {
    if (inclusive) value >= start && value <= end
    else value > start && value < end
  }
}

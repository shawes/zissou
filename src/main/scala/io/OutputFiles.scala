package io

class OutputFiles(val includeLarvaeMovements: Boolean, val path: String, val prefix : String, val percent: Double) {
  def this() = this(false,"","",0)
}

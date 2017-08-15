package io

import locals.ShapeFileType
import locals.ShapeFileType.ShapeFileType

class OutputFiles(val includeLarvaeMovements: Boolean, val shapeType: ShapeFileType, val path: String, val percent: Double) {
  def this() = this(false, ShapeFileType.Line, " ",0)


}

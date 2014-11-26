package physical

class Grid(var width: Int, var height: Int, var depth: Int, var cell: Cell) {
  def this() = this(0, 0, 0, new Cell(0, 0, 0))

  def totalCellCount: Int = width * height * depth

  def layerCellCount: Int = width * height
}

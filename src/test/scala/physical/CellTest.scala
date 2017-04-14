package physical

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class CellTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "A cell" should "not be null" in {
    val cell = new Cell(3,4,5)
    assert(cell != null)
  }

  it should "store the width as a double" in {
    val cell = new Cell(3,4,5)
    assert(cell.width == 3.0)
  }

  it should "store the height as a double" in {
    val cell = new Cell(3,4,5)
    assert(cell.height == 4.0)
  }

  it should "store the depth as a double" in {
    val cell = new Cell(3,4,5)
    assert(cell.depth == 5.0)
  }

}

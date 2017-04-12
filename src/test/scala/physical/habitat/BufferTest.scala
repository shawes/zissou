package physical.habitat

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class BufferTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  val buffer = new Buffer(true, 5)

  "A buffer" should "not be null upon construction" in {
    assert(buffer != null)
  }

  it should "contain an empty list upon construction" in {
    assert(buffer.bufferShapes.isEmpty)
  }

  it should "know when its buffered" in {
    assert(buffer.isBuffered)
  }

  it should "have a size" in {
    assert(buffer.size == 5.0)
  }

}

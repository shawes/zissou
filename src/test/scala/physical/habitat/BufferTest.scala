package physical.habitat

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class BufferTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  val buffer = new Buffer(true, 5, 10)

  "A buffer" should "not be null upon construction" in {
    assert(buffer != null)
  }

  it should "contain empty lists upon construction" in {
    assert(buffer.settlementBufferShapes.isEmpty)
    assert(buffer.olfactoryBufferShapes.isEmpty)
  }

  it should "know when its buffered" in {
    assert(buffer.isBuffered)
  }

  it should "have a settlement buffer size" in {
    assert(buffer.settlement == 5.0)
  }

  it should "have a olfactory buffer size" in {
    assert(buffer.olfactory == 10.0)
  }

}

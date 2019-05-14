package biology.swimming

import locals._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import scala.language.reflectiveCalls

class HorizontalSwimmingTest
    extends FlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  def fixture =
    new {
      val swimmingStrategy1 =
        new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1, 1)
      val swimmingStrategy2 =
        new HorizontalSwimming(Directed, StrategyTwo, 1, 1, 1, true, 1, 1)
      val swimmingStrategy3 =
        new HorizontalSwimming(Directed, StrategyThree, 1, 1, 1, true, 1, 1)
    }

  "A horizontal swimming object" should "initialise" in {
    val f = fixture
    assert(f.swimmingStrategy1 != null)
  }

  it should "choose strategy 1 if initialised" in {
    val f = fixture
    val speed = f.swimmingStrategy1(new HorizontalSwimmingVariables(1, 1, 1, 1))
    assert(speed.u > 0)
  }

}

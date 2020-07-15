package biology.swimming

import locals._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._

class HorizontalSwimmingTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  def fixture =
    new {
      val swimmingStrategy1 =
        new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)
      val swimmingStrategy2 =
        new HorizontalSwimming(Directed, StrategyTwo, 1, 1, 1, true, 1)
      val swimmingStrategy3 =
        new HorizontalSwimming(Directed, StrategyThree, 1, 1, 1, true, 1)
    }

  "A horizontal swimming object" should "initialise" in {
    val swimmingStrategy1 =
      new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)
    assert(swimmingStrategy1 != null)
  }

  it should "choose strategy 1 if initialised" in {
    val swimmingStrategy1 =
      new HorizontalSwimming(Directed, StrategyOne, 1, 1, 1, true, 1)
    val speed =
      swimmingStrategy1(new HorizontalSwimmingVariables(1, 1, 1, 1, 1, 1))
    assert(!speed.isDefined)
  }
}

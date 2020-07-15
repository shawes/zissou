package physical

import locals._
import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._

class TimeStepTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester
    with Matchers {

  "The timestep" should " not initialise to null" in {
    val result = new TimeStep(1.0, "hour")
    result should not equal null
  }

  it should "initialise to parameters" in {
    val result = new TimeStep(1.0, "hour")
    result.duration should equal(1.0)
    result.timePeriod should equal("hour")
  }

  it should "calculate seconds in an hour" in {
    val result = new TimeStep(1.0, "hour")
    result.totalSeconds should equal(3600)
  }

  it should "calculate seconds in two hours" in {
    val result = new TimeStep(2.0, "hour")
    result.totalSeconds should equal(7200)
  }

  it should "calculate seconds in an second" in {
    val result = new TimeStep(1.0, "second")
    result.totalSeconds should equal(1.0)
  }

  it should "calculate seconds in an day" in {
    val result = new TimeStep(1.0, "day")
    result.totalSeconds should equal(86400)
  }

}

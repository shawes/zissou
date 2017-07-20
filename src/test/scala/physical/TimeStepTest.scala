package physical

import locals.TimeStepType
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class TimeStepTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "The timestep" should " not initialise to null" in {
    val result = new TimeStep(1.0, TimeStepType.Hour)
    result should not equal null
  }

  it should "initialise with no parameters" in {
    val result = new TimeStep()
    result should not equal null
  }

  it should "initialise to parameters" in {
    val result = new TimeStep(1.0, TimeStepType.Hour)
    result.duration should equal(1.0)
    result.timeType should equal(TimeStepType.Hour)
  }

  it should "calculate seconds in an hour" in {
    val result = new TimeStep(1.0, TimeStepType.Hour)
    result.totalSeconds should equal(3600)
  }

  it should "calculate seconds in two hours" in {
    val result = new TimeStep(2.0, TimeStepType.Hour)
    result.totalSeconds should equal(7200)
  }

  it should "calculate seconds in an second" in {
    val result = new TimeStep(1.0, TimeStepType.Second)
    result.totalSeconds should equal(1.0)
  }

  it should "calculate seconds in an day" in {
    val result = new TimeStep(1.0, TimeStepType.Day)
    result.totalSeconds should equal(86400)
  }


}

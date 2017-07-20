package physical

import maths.Random
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class TurbulenceTest extends FlatSpec with MockitoSugar with PrivateMethodTester {


  "The turbulence object" should "initialise" in {
    val turbulence = new Turbulence()
    turbulence should not be null
  }

  it should "initialise the correct horizontal turbulence" in {
    val mockRandom = mock[Random]
    when(mockRandom.get).thenReturn(4)
    val turbulence = new Turbulence(1.0, 2.0, 3, mockRandom)
    val h_turb_cal: Double = Math.pow((2.0 * 1.0) / 3, 0.5)
    turbulence.horizontalTurbulence should equal(h_turb_cal)
  }

  it should "initialise the correct vertical turbulence" in {
    val mockRandom = mock[Random]
    when(mockRandom.get).thenReturn(4)
    val turbulence = new Turbulence(1.0, 2.0, 3, mockRandom)
    val v_turb_cal = Math.pow((2.0 * 2.0) / 3, 0.5)
    turbulence.verticalTurbulence should equal(v_turb_cal)
  }

  it should "apply the correct vertical turbulence" in {
    val mockRandom = mock[Random]
    when(mockRandom.get).thenReturn(0.2)
    val turbulence = new Turbulence(1.0, 2.0, 3, mockRandom)
    val v_turb_cal = Math.pow((2.0 * 2.0) / 3, 0.5) * 0.2
    val velocity = new Velocity(5.0, 7.0, 9.0)
    val result = turbulence.apply(velocity)
    result.w should equal(velocity.w + v_turb_cal)
  }

  it should "apply the correct horizontal turbulence" in {
    val mockRandom = mock[Random]
    when(mockRandom.get).thenReturn(0.2)
    val turbulence = new Turbulence(1.0, 2.0, 3, mockRandom)
    val h_turb_cal = Math.pow((2.0 * 1.0) / 3, 0.5) * 0.2
    val velocity = new Velocity(5.0, 7.0, 9.0)
    val result = turbulence.apply(velocity)
    result.u should equal(velocity.u + h_turb_cal)
    result.v should equal(velocity.v + h_turb_cal)
  }


  it should "apply equal vertical & horizontal coefficients" in {
    val mockRandom = mock[Random]
    when(mockRandom.get).thenReturn(1)
    val turbulence = new Turbulence(2, 2, 1, mockRandom)
    val velocity = new Velocity(1, 2, 3)
    val result = turbulence.apply(velocity)

    assert(result.u == 3)
    assert(result.v == 4)
    assert(result.w == 5)
  }

}

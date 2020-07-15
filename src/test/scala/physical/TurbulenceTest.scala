package physical

import maths.Random
import org.mockito.Mockito._
import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._

class TurbulenceTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester
    with Matchers {

  "The turbulence object" should "initialise" in {
    val turbulence = new Turbulence()
    turbulence should not be null
  }

  it should "initialise the correct horizontal turbulence" in {
    //val mockRandom = mock[Random]
    //when(mockRandom.get).thenReturn(4)
    val turbulence = new Turbulence(300, 15, 3600)
    val h_turbulence_cal: Double = Math.pow((2.0 * 300) / 3600, 0.5)
    turbulence.horizontalTurbulence should equal(h_turbulence_cal)
  }

  it should "initialise the correct vertical turbulence" in {
    //val mockRandom = mock[Random]
    //when(mockRandom.get).thenReturn(4)
    val turbulence = new Turbulence(1.0, 2.0, 3)
    val v_turbulence_cal = Math.pow((2.0 * 2.0) / 3, 0.5)
    turbulence.verticalTurbulence should equal(v_turbulence_cal)
  }

  // it should "apply the correct vertical turbulence" in {
  //   val mockRandom = mock[Random]
  //   when(mockRandom.get).thenReturn(0.2)
  //   val turbulence = new Turbulence(1.0, 2.0, 3)
  //   val v_turbulence_cal = Math.pow((2.0 * 2.0) / 3, 0.5) * 0.2
  //   val velocity = new Velocity(5.0, 7.0, 9.0)
  //   val result = turbulence.apply(velocity)
  //   result.w should not equal(3)
  // }
  //
  // it should "apply the correct horizontal turbulence" in {
  //   val mockRandom = mock[Random]
  //   when(mockRandom.get).thenReturn(0.2)
  //   val turbulence = new Turbulence(1.0, 2.0, 3)
  //   val h_turbulence_cal = Math.pow((2.0 * 1.0) / 3, 0.5) * 0.2
  //   val velocity = new Velocity(5.0, 7.0, 9.0)
  //   val result = turbulence.apply(velocity)
  //   result.u should equal(velocity.u + h_turbulence_cal)
  //   result.v should equal(velocity.v + h_turbulence_cal)
  // }
  //
  //
  // it should "apply equal vertical & horizontal coefficients" in {
  //   val mockRandom = mock[Random]
  //   when(mockRandom.get).thenReturn(1)
  //   val turbulence = new Turbulence(2, 2, 1)
  //   val velocity = new Velocity(1, 2, 3)
  //   val result = turbulence.apply(velocity)
  //
  //   assert(result.u == 3)
  //   assert(result.v == 4)
  //   assert(result.w == 5)
  // }

}

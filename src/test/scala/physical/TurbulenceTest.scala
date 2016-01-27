package physical

import maths.RandomNumberGenerator
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class TurbulenceTest extends FlatSpec with MockitoSugar with PrivateMethodTester {


  "The turbulence object" should "initialise" in {
    val turbulence = new Turbulence()
    turbulence should not be null
  }

  it should "initialise to zero with no parameters" in {
    val turbulence = new Turbulence()
    assert(turbulence.horizontalDiffusionCoefficient == 0)
    assert(turbulence.verticalDiffusionCoefficient == 0)
  }

  it should "initialise to passed parameters" in {
    val turbulence = new Turbulence(2, 3, new RandomNumberGenerator(1))
    assert(turbulence.horizontalDiffusionCoefficient == 2)
    assert(turbulence.verticalDiffusionCoefficient == 3)
  }

  it should "apply equal vertical & horizontal coefficients" in {
    val mockRandom = mock[RandomNumberGenerator]
    when(mockRandom.get).thenReturn(1)
    val turbulence = new Turbulence(2, 2, mockRandom)
    val velocity = new Velocity(1, 2, 3)
    val result = turbulence.apply(velocity)

    assert(result.u == 3)
    assert(result.v == 4)
    assert(result.w == 5)
  }

  it should "apply different vertical & horizontal coefficients" in {
    val mockRandom = mock[RandomNumberGenerator]
    when(mockRandom.get).thenReturn(2)
    val turbulence = new Turbulence(2, 3, mockRandom)
    val velocity = new Velocity(1, 2, 3)
    val result = turbulence.apply(velocity)

    assert(result.u == 5)
    assert(result.v == 6)
    assert(result.w == 9)
  }
}

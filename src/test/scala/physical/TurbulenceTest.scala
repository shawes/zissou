package physical

import org.apache.commons.math3.random.MersenneTwister
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
    turbulence.horizontalDiffusionCoefficient should equal(0)
    turbulence.verticalDiffusionCoefficient should equal(0)

  }

  it should "initialise to passed parameters" in {
    val turbulence = new Turbulence(2, 3)
    turbulence.horizontalDiffusionCoefficient should equal(2)
    turbulence.verticalDiffusionCoefficient should equal(3)
  }

  it should "apply equal vertical & horizontal coefficients" in {
    val turbulence = new Turbulence(2, 2)
    val mockRandom = mock[MersenneTwister]
    when(mockRandom.nextDouble()).thenReturn(1)
    turbulence.random = mockRandom
    val velocity = new Velocity(1, 2, 3)
    val result = turbulence.apply(velocity)

    assert(result.u == 3)
    assert(result.v == 4)
    assert(result.w == 5)
  }

  it should "apply different vertical & horizontal coefficients" in {
    val turbulence = new Turbulence(2, 3)
    val mockRandom = mock[MersenneTwister]
    when(mockRandom.nextDouble()).thenReturn(2)
    turbulence.random = mockRandom
    val velocity = new Velocity(1, 2, 3)
    val result = turbulence.apply(velocity)

    assert(result.u == 5)
    assert(result.v == 6)
    assert(result.w == 9)
  }
}

package main

import org.scalatest.FlatSpec
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import maths.integration.RungeKuttaIntegration
import io.config.Configuration

class BiologicalModelTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  // "Biological model" should "initialise" in {
  //   val mockIntegrator = mock[RungeKuttaIntegration]
  //   val mockClock = mock[SimulationClock]
  //   val mockConfig = mock[Configuration]
  //   val model = new BiologicalModel(mockConfig, mockClock, mockIntegrator)
  //   assert(model != null)
  // }

  // it should "...." in {
  //
  // }

}

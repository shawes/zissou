package main

import org.scalatest._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._
import maths.integration.RungeKuttaIntegration
import io.config.Configuration

class BiologicalModelTest
    extends flatspec.AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

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

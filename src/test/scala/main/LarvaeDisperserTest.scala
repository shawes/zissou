package main

import io.config.Configuration
import org.scalatest.{FlatSpec, PrivateMethodTester}

class LarvaeDisperserTest extends FlatSpec with PrivateMethodTester {

  val config = new Configuration()

  "A larval disperser" should "require a non-null configuration file" in {
    intercept[IllegalArgumentException] {
      new LarvaeDisperser(null)
    }
  }

  it should "calculate the mortality rate using the step" in {
    val ld = new LarvaeDisperser(new Configuration())

    val calculateMortalityRate = PrivateMethod[Double]('calculateMortalityRate)
    ld invokePrivate calculateMortalityRate(1)

  }
}

package physical

import com.github.nscala_time.time.Imports._
import org.apache.commons.math3.random.MersenneTwister

import scala.compat.Platform

class Turbulence(val horizontalDiffusionCoefficient: Double, val verticalDiffusionCoefficient: Double) {
  val random = new MersenneTwister(Platform.currentTime)

  def this() = this(0, 0)

  def apply(v: Velocity, t: Duration) : Unit = {
    val horizontalTurbulence = horizontalDiffusionCoefficient * random.nextDouble()
    val verticalTurbulence = verticalDiffusionCoefficient * random.nextDouble()
    new Velocity(v.u + horizontalTurbulence, v.v + horizontalTurbulence, v.w + verticalTurbulence)
  }

}

package physical

import org.apache.commons.math.random.MersenneTwister
import scala.compat.Platform
import com.github.nscala_time.time.Imports._

class Turbulence(val horizontalDiffusionCoefficient: Double, val verticalDiffusionCoefficient: Double) {
  def this() = this(0, 0)


  val random = new MersenneTwister(Platform.currentTime)

  def apply(v: Velocity, t: Duration) : Unit = {
    val horizontalTurbulence = horizontalDiffusionCoefficient * random.nextDouble()
    val verticalTurbulence = verticalDiffusionCoefficient * random.nextDouble()
    new Velocity(v.u + horizontalTurbulence, v.v + horizontalTurbulence, v.w + verticalTurbulence)
  }

}

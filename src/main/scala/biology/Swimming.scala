package biology

import locals.SwimmingAbility._
import locals.SwimmingAbility
import physical.Velocity
import maths.RandomNumberGenerator

class Swimming(val ability: SwimmingAbility,
               val criticalSwimmingSpeed: Double,
                val inSituSimmingSpeed: Double,
                val endurance: Double,
                val reynoldsEffect: Boolean) {

  def apply(angle : Double, speed : Double): Velocity = {
    val uOrientated = speed * math.cos(angle) * RandomNumberGenerator.get
    val vOrientated = speed * math.sin(angle) * RandomNumberGenerator.get
    new Velocity(uOrientated, vOrientated, 0)
  }
  
  def isDirected: Boolean = ability == SwimmingAbility.Directed
}

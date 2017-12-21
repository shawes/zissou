package biology

import locals.SwimmingAbility._
import locals.SwimmingAbility
import physical.Velocity
import maths.RandomNumberGenerator

class Swimming(val ability: SwimmingAbility,
               val criticalSwimmingSpeed: Double,
               val inSituSwimmingPotential: Double,
               val endurance: Double,
               val reynoldsEffect: Boolean) {

  def apply(angle : Double): Velocity = {
    val speed = criticalSwimmingSpeed * RandomNumberGenerator.get(inSituSwimmingPotential,1) * endurance
    val uOrientated = speed * math.cos(angle)
    val vOrientated = speed * math.sin(angle)
    new Velocity(uOrientated, vOrientated, 0)
  }

  def isDirected: Boolean = ability == SwimmingAbility.Directed
}

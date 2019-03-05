package biology

import locals.SwimmingAbility._
import locals.SwimmingAbility
import physical.Velocity
import maths.RandomNumberGenerator

class Swimming(val ability: SwimmingAbility,
               val criticalSwimmingSpeed: Double,
               val inSituSwimmingPotential: Double,
               val endurance: Double,
               val reynoldsEffect: Boolean,
               val ageMaxSpeedReached: Int,
               val hatchSwimmingSpeed : Double) {

  def apply(angle : Double): Velocity = {
    val speed = criticalSwimmingSpeed * RandomNumberGenerator.get(inSituSwimmingPotential,1) * endurance
    val uOrientated = speed * math.cos(angle)
    val vOrientated = speed * math.sin(angle)
    new Velocity(uOrientated, vOrientated, 0)
  }

  def apply(angle : Double, age : Int, preflexion : Int): Velocity = {
    val speed = getSpeedWolanski2014(age, preflexion)
    val uOrientated = speed * math.cos(angle)
    val vOrientated = speed * math.sin(angle)
    new Velocity(uOrientated, vOrientated, 0)
  }

  private def getSpeedWolanski2014(age : Int, preflexion : Int): Double = {
    if(age < ageMaxSpeedReached) {
      criticalSwimmingSpeed*(age - preflexion) / (ageMaxSpeedReached - preflexion)
    } else {
      criticalSwimmingSpeed
    }
  }

  private def getSpeedStaaterman2012(age : Int, pld : Int) : Double = {
    hatchSwimmingSpeed + Math.pow(10,Math.log(age)/Math.log(pld)*Math.log(criticalSwimmingSpeed-hatchSwimmingSpeed))
  }

  def isDirected: Boolean = ability == SwimmingAbility.Directed
}

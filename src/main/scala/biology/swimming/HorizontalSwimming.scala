package biology.swimming

import locals.SwimmingAbility._
import locals.SwimmingAbility
import physical.Velocity
import maths.RandomNumberGenerator

class HorizontalSwimming(config : HorizontalSwimmingConfig) {

  def apply(variables : HorizontalSwimmingVariables) = {
    val speed = 0 //TODO Complete this pattern
    val uOrientated = speed * math.cos(variables.angle)
    val vOrientated = speed * math.sin(variables.angle)
    new Velocity(uOrientated, vOrientated, 0)
  }

  def firstImpl(variables : HorizontalSwimmingVariables) : Double = {
    config.criticalSwimmingSpeed * 
    RandomNumberGenerator.get(config.inSituSwimmingPotential,1) * 
    config.endurance
  }

  // From Wolanski 2014
  def secondImpl(variables : HorizontalSwimmingVariables) : Double = {
    if(variables.age < config.ageMaxSpeedReached) {
      config.criticalSwimmingSpeed*(variables.age - variables.preflexion) / (config.ageMaxSpeedReached - variables.preflexion)
    } else {
      config.criticalSwimmingSpeed
    }
  }

  /* From Staaterman 2012 */
  def thirdImpl(variables : HorizontalSwimmingVariables) : Double = {
    config.hatchSwimmingSpeed + Math.pow(10,Math.log(variables.age)/Math.log(variables.pld)*Math.log(config.criticalSwimmingSpeed-config.hatchSwimmingSpeed))
  }

  def isDirected: Boolean = config.ability == SwimmingAbility.Directed
}

class HorizontalSwimmingConfig(val ability: SwimmingAbility,
               val criticalSwimmingSpeed: Double,
               val inSituSwimmingPotential: Double,
               val endurance: Double,
               val reynoldsEffect: Boolean,
               val ageMaxSpeedReached: Int,
               val hatchSwimmingSpeed : Double)  {}

class HorizontalSwimmingVariables(val angle: Double, val age : Int, val preflexion : Int, val pld: Int)

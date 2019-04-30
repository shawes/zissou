package biology.swimming

import locals._
import physical.Velocity
import maths.RandomNumberGenerator

class HorizontalSwimming(config: HorizontalSwimmingConfig) {

  def apply(
      variables: HorizontalSwimmingVariables
  ) = {
    val speed = config.strategy match {
      case StrategyOne   => getSpeed(typeOneSwimmingSpeed, variables)
      case StrategyTwo   => getSpeed(typeTwoSwimmingSpeed, variables)
      case StrategyThree => getSpeed(typeTwoSwimmingSpeed, variables)
    }
    val uOrientated = speed * math.cos(variables.angle)
    val vOrientated = speed * math.sin(variables.angle)
    new Velocity(uOrientated, vOrientated, 0)
  }

  def getSpeed(
      impl: (HorizontalSwimmingVariables) => Double,
      variables: HorizontalSwimmingVariables
  ) = impl(variables)

  def typeOneSwimmingSpeed(variables: HorizontalSwimmingVariables): Double = {
    config.criticalSwimmingSpeed *
      RandomNumberGenerator.get(config.inSituSwimmingPotential, 1) *
      config.endurance
  }

  // From Wolanski 2014
  def typeTwoSwimmingSpeed(variables: HorizontalSwimmingVariables): Double = {
    if (variables.age < config.ageMaxSpeedReached) {
      config.criticalSwimmingSpeed * (variables.age - variables.preflexion) / (config.ageMaxSpeedReached - variables.preflexion)
    } else {
      config.criticalSwimmingSpeed
    }
  }

  /* From Staaterman 2012 */
  def typeThreeSwimmingSpeed(variables: HorizontalSwimmingVariables): Double = {
    config.hatchSwimmingSpeed + Math.pow(
      10,
      Math.log(variables.age) / Math.log(variables.pld) * Math.log(
        config.criticalSwimmingSpeed - config.hatchSwimmingSpeed
      )
    )
  }

  def isDirected: Boolean = config.ability == Directed
}

class HorizontalSwimmingConfig(
    val ability: SwimmingAbility,
    val strategy: HorizontalSwimmingStrategy,
    val criticalSwimmingSpeed: Double,
    val inSituSwimmingPotential: Double,
    val endurance: Double,
    val reynoldsEffect: Boolean,
    val ageMaxSpeedReached: Int,
    val hatchSwimmingSpeed: Double
) {}

class HorizontalSwimmingVariables(
    val angle: Double,
    val age: Int,
    val preflexion: Int,
    val pld: Int
)

package biology.swimming

import locals._
import physical.Velocity
import maths.RandomNumberGenerator

class HorizontalSwimming(
    val ability: SwimmingAbility,
    val strategy: HorizontalSwimmingStrategy,
    val criticalSwimmingSpeed: Double,
    val inSituSwimmingPotential: Double,
    val endurance: Double,
    val reynoldsEffect: Boolean,
    val ageMaxSpeedReached: Int,
    val hatchSwimmingSpeed: Double
) {

  def apply(
      variables: HorizontalSwimmingVariables
  ) = {
    val speed = strategy match {
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

  private def typeOneSwimmingSpeed(
      variables: HorizontalSwimmingVariables
  ): Double = {
    criticalSwimmingSpeed *
      RandomNumberGenerator.get(inSituSwimmingPotential, 1) *
      endurance
  }

  // From Wolanski 2014
  private def typeTwoSwimmingSpeed(
      variables: HorizontalSwimmingVariables
  ): Double = {
    if (variables.age < ageMaxSpeedReached) {
      criticalSwimmingSpeed * (variables.age - variables.preflexion) / (ageMaxSpeedReached - variables.preflexion)
    } else {
      criticalSwimmingSpeed
    }
  }

  /* From Staaterman 2012 */
  private def typeThreeSwimmingSpeed(
      variables: HorizontalSwimmingVariables
  ): Double = {
    hatchSwimmingSpeed + Math.pow(
      10,
      Math.log(variables.age) / Math.log(variables.pld) * Math.log(
        criticalSwimmingSpeed - hatchSwimmingSpeed
      )
    )
  }

  def isDirected: Boolean = ability == Directed
}

class HorizontalSwimmingVariables(
    val angle: Double,
    val age: Int,
    val preflexion: Int,
    val pld: Int
)

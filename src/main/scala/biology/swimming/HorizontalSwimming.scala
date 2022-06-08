package biology.swimming

import locals.Constants
import locals.Constants._
import locals.Enums.SwimmingStrategy
import locals.Enums.Swims
import biology.OntogenyFish
import physical.Velocity
import utilities._
import maths.RandomNumberGenerator
import grizzled.slf4j.Logging

class HorizontalSwimming(
    val ability: Swims,
    val strategy: SwimmingStrategy,
    val criticalSwimmingSpeed: Double,
    val inSituSwimmingPotential: Double,
    val endurance: Double,
    val reynoldsEffect: Boolean,
    // val ageMaxSpeedReached: Int,
    val hatchSwimmingSpeed: Double
) extends Logging {

  def apply(
      variables: HorizontalSwimmingVariables
  ): Option[Velocity] = {
    val speed = strategy match {
      case SwimmingStrategy.One   => getSpeed(typeOneSwimmingSpeed, variables)
      case SwimmingStrategy.Two   => getSpeed(typeTwoSwimmingSpeed, variables)
      case SwimmingStrategy.Three => getSpeed(typeThreeSwimmingSpeed, variables)
    }
    if (speed > 0) then {
      if (isDirected && variables.angle != LightWeightException.NoSwimmingAngleException) then {
        Some(calculateVelocity(variables.angle, speed))
      } else {
        Some(calculateVelocity(RandomNumberGenerator.getAngle, speed))
      }
    } else {
      None
    }
  }

  private def calculateVelocity(angle: Double, speed: Double): Velocity = {
    val uOrientated = speed * math.cos(angle)
    val vOrientated = speed * math.sin(angle)
    new Velocity(uOrientated, vOrientated, 0)
  }

  def getSpeed(
      impl: (HorizontalSwimmingVariables) => Double,
      variables: HorizontalSwimmingVariables
  ) = impl(variables)

  private def typeOneSwimmingSpeed(
      variables: HorizontalSwimmingVariables
  ): Double = {
    if (variables.age > variables.flexion) then {
      criticalSwimmingSpeed *
        RandomNumberGenerator.get(inSituSwimmingPotential, 1) *
        endurance
    } else {
      0
    }
  }

  // From Wolanski 2014
  private def typeTwoSwimmingSpeed(
      variables: HorizontalSwimmingVariables
  ): Double = {
    if (variables.age < variables.postflexion) then {
      criticalSwimmingSpeed * (variables.age - variables.preflexion) / (variables.postflexion - variables.preflexion)
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
      (Math.log10(Time.convertSecondsToDays(variables.age)) / Math.log10(
        Time.convertSecondsToDays(variables.pld)
      )) *
        Math.log10(criticalSwimmingSpeed * 100 - hatchSwimmingSpeed * 100)
    ) / 100
  }

  def isDirected: Boolean = ability == Swims.Directed
}

class HorizontalSwimmingVariables(
    val angle: Double,
    val age: Int,
    val preflexion: Int,
    val flexion: Int,
    val postflexion: Int,
    val pld: Int
)

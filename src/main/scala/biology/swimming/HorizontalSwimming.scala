package biology.swimming

import locals._
import locals.Constants._
import biology.OntogenyFish
import physical.Velocity
import utilities._
import maths.RandomNumberGenerator
import grizzled.slf4j.Logging

class HorizontalSwimming(
    val ability: SwimmingAbility,
    val strategy: HorizontalSwimmingStrategy,
    val criticalSwimmingSpeed: Double,
    val inSituSwimmingPotential: Double,
    val endurance: Double,
    val reynoldsEffect: Boolean,
    //val ageMaxSpeedReached: Int,
    val hatchSwimmingSpeed: Double
) extends Logging {

  // A passive larva does not swim, whatever its swimming speed
  def apply(
      variables: HorizontalSwimmingVariables
  ): Option[Velocity] =
    if (ability == Passive) None else swim(variables)

  private def swim(
      variables: HorizontalSwimmingVariables
  ): Option[Velocity] = {
    val speed = strategy match {
      case StrategyOne   => getSpeed(typeOneSwimmingSpeed, variables)
      case StrategyTwo   => getSpeed(typeTwoSwimmingSpeed, variables)
      case StrategyThree => getSpeed(typeThreeSwimmingSpeed, variables)
    }
    if (speed > 0) {
      if (isDirected && variables.angle != LightWeightException.NoSwimmingAngleException) {
        Some(calculateVelocity(variables.angle, speed))
      } else {
        Some(calculateVelocity(RandomNumberGenerator.getAngle, speed))
      }
    } else {
      None
    }
  }

  // Angles are kept in degrees, counter-clockwise from east, as the habitat reports the
  // direction to a reef and as RandomNumberGenerator.getAngle draws them. sin and cos take
  // radians: given degrees they send a larva off at an unrelated heading (45 became 58,
  // 180 became 233), so a larva told to swim at a reef did not.
  private def calculateVelocity(angleInDegrees: Double, speed: Double): Velocity = {
    val angle = math.toRadians(angleInDegrees)
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
    if (variables.age > variables.flexion) {
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
    if (variables.age < variables.postflexion) {
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

  def isDirected: Boolean = ability == Directed
}

class HorizontalSwimmingVariables(
    // the heading to swim at, in degrees counter-clockwise from east, or
    // LightWeightException.NoSwimmingAngleException when there is none
    val angle: Double,
    val age: Int,
    val preflexion: Int,
    val flexion: Int,
    val postflexion: Int,
    val pld: Int
)

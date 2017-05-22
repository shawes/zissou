package biology

import physical.Velocity
import maths.RandomNumberGenerator

class Swimming() {
  def apply(v: Velocity, angle : Double, speed : Double): Velocity = {
    val uOrientated = speed * math.cos(angle) * RandomNumberGenerator.get
    val vOrientated = speed * math.sin(angle) * RandomNumberGenerator.get
    new Velocity(v.u + uOrientated, v.v + vOrientated, v.w)
  }
}

package physical

class Velocity(var u: Double, var v: Double, var w: Double) {
  def this(u: Double, v: Double) = this(u, v, 0)

  def this() = this(0, 0, 0)

  def +(that: Velocity): Velocity =
    new Velocity(u + that.u, v + that.v, w + that.w)

  def -(that: Velocity): Velocity =
    new Velocity(u - that.u, v - that.v, w - that.w)

  def *(that: Velocity): Velocity =
    new Velocity(u * that.u, v * that.v, w * that.w)

  def *(scalar: Double): Velocity =
    new Velocity(u * scalar, v * scalar, w * scalar)

  def ==(that: Velocity): Boolean =
    u == that.u && v == that.v && w == that.w

  def isUndefined: Boolean = {
    u.isNaN || v.isNaN
  }

  override def toString = "u = %.2f, v = %.2f, w = %.2f".format(u, v, w)
}

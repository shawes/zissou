package physical

class Velocity(var u: Double, var v: Double, var w: Double) {
  def this(u: Double, v: Double) = this(u, v, 0)

  //def this() = this(0, 0, 0)

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

  def isDefined: Boolean = !isUndefined

  def isUndefined: Boolean = {
    u.isNaN || v.isNaN || w.isNaN
  }

  override def toString = "u = %.4f, v = %.4f, w = %.4f".format(u, v, w)
}

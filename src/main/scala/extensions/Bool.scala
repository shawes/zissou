package extensions

import scala.language.implicitConversions

case class Bool(b: Boolean) {
  def ?[X](t: => X) = new {
    def |(f: => X) = if (b) then t else f
  }
}

object Bool {
  implicit def BooleanBool(b: Boolean): Bool = Bool(b)
}

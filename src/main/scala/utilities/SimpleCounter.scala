package utilities

case class SimpleCounter(count: Int) {
  def increment = copy(count = count + 1)

  //def dec = copy(count = count - 1)
}


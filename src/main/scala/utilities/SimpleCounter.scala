package utilities

case class SimpleCounter(count: Int) {
  def increment(): Unit = copy(count = count + 1)

  def increase(amount: Int): Unit = copy(count = count + amount)
}


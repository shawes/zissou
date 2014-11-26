package io

object Logger {
  private val Suppress: Boolean = false

  def info(str: String) = if (!Suppress) println("[info] " + str)

  def warning(str: String) = if (!Suppress) println("[warning] " + str)

  def severe(str: String) = if (!Suppress) println("[severe] " + str)
}

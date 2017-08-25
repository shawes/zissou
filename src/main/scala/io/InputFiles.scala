package io

import com.github.nscala_time.time.Imports._

class InputFiles(val flowFilePath: String, val habitatFilePath: String, val flowFiles: Array[String]) {
  def this() = this("", "", Array.empty[String])
}

package io

import com.github.nscala_time.time.Imports._

class InputFiles(val flowFilePath: String, val habitatFilePath: String, val flowFiles: Array[String], val randomSeed : Option[Int]) {
  def this() = this("", "", Array.empty[String], None)
}

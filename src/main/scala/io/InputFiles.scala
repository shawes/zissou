package io

import com.github.nscala_time.time.Imports._

class InputFiles(val flowFilePath: String, val habitatFilePath: String, val flowFiles: Array[String]) {
  def this() = this("", "", Array.empty[String])

  def getFlowDateFromFile(index: Int): DateTime = {
    val filename = flowFiles(index)
    convertFileNameToDateTime(filename)
  }

  def convertFileNameToDateTime(filename: String): DateTime = {
    val splitOnDotSeparator = filename.split('.')
    val splitOnUnderscore = splitOnDotSeparator(0).split('_')
    val splitOnHyphen = splitOnUnderscore(1).split('-')

    new DateTime(splitOnHyphen(2).toInt, splitOnHyphen(1).toInt, splitOnHyphen(0).toInt, 0, 0, 0, 0)

  }
}

package io

import org.scalatest.FlatSpec

class InputFilesTest extends FlatSpec {

  "The flow date" should "be read from the filename" in {
    val filename = "flowData_21-01-2013.xml"
    val inputFiles = new InputFiles(" ", " ", null)
    val date = inputFiles.ConvertFileNameToDateTime(filename)
    assert(date.getYear == 2013)
    assert(date.getMonthOfYear == 1)
    assert(date.getDayOfMonth == 21)
  }
}

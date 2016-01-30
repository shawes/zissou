package io

import java.io.{BufferedWriter, File}

import biology.Larva
import grizzled.slf4j.Logging

class LarvaeFileWriter(larvae: List[Larva], file: File) extends FileWriterTrait with Logging {

  val columnHeaders = "id,born,age,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"

  def write(): Unit = {
    info("Writing " + larvae.size + " larvae")
    val bw = new BufferedWriter(new java.io.FileWriter(file))
    bw.write(columnHeaders)
    bw.newLine()
    larvae.foreach(larvae => bw.write(writeCsvRow(larvae)))
    bw.close()
  }

  private def writeCsvRow(larva: Larva): String = {
    debug(larva.toString)
    var distance = 0
    val sb = new StringBuilder()
    larva.history.foreach(hist => {
      sb append larva.id + ","
      sb append larva.birthday + ","
      sb append hist.age + ","
      sb append larva.pelagicLarvalDuration + ","
      sb append larva.birthplace.name + ","
      sb append hist.state + ","
      sb append hist.habitat.habitat + ","
      sb append hist.habitat.habitat.toString + ","
      sb append hist.position.latitude + ","
      sb append hist.position.longitude + ","
      sb append hist.position.depth + ",\n"
    })

    val csvRow = sb.toString()
    debug(csvRow)
    csvRow

  }

}

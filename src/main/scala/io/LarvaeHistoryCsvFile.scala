package io

import java.io.{BufferedWriter, File}

import biology.Larva
import grizzled.slf4j.Logging

class LarvaeHistoryCsvFile(larvae: List[List[Larva]], filepath: String) extends Logging {

  val columnHeaders = "id,born,age,stage,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"

  def write(): Unit = {
    info("Writing " + larvae.size + " larvae")
    var count = 1
    for (larvaeList <- larvae) {
      val bw = new BufferedWriter(new java.io.FileWriter(new File(filepath + "//larvae_log_batch" + count + ".csv")))
      bw.write(columnHeaders)
      bw.newLine()
      larvaeList.foreach(larva => bw.write(writeCsvRow(larva)))
      bw.close()
      count += 1
    }
  }

  private def writeCsvRow(larva: Larva): String = {
    debug(larva.toString)
    var distance = 0
    val sb = new StringBuilder()
    larva.history.foreach(hist => {
      sb append larva.id + ","
      sb append larva.birthday + ","
      sb append hist.age + ","
      sb append hist.stage + ","
      sb append larva.pelagicLarvalDuration + ","
      sb append larva.birthplace.name + ","
      sb append hist.state + ","
      if(hist.habitat.nonEmpty) {
        sb append hist.habitat.get.id + ","
        sb append hist.habitat.get.habitat.toString + ","
      } else {
        sb append "-1,"
        sb append "ocean,"
      }
      sb append hist.position.latitude + ","
      sb append hist.position.longitude + ","
      sb append hist.position.depth + ",\n"
    })

    val csvRow = sb.toString().toLowerCase()
    //debug(csvRow)
    csvRow

  }

}
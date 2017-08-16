package io

import java.io.{BufferedWriter, File}

import biology.Larva
import grizzled.slf4j.Logging

class LarvaeHistoryCsvFile(larvae: List[Larva], filepath: String) extends Logging {

  val columnHeaders = "id,born,age,stage,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"

  def write(): Unit = {
    info("Writing " + larvae.size + " larvae")
    //var count = 1
    val bw = new BufferedWriter(new java.io.FileWriter(new File(filepath + "//larvae-log-batch.csv")))
    for (larva <- larvae) {
      bw.write(columnHeaders)
      bw.newLine()
      bw.write(writeCsvRow(larva))
      //count += 1
    }
    bw.close()
  }

  private def writeCsvRow(larva: Larva): String = {
    debug(larva.toString)
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
        sb append hist.habitat + ","
      } else {
        sb append "-1,"
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

package io

import java.io.{BufferedWriter, File}

import biology.Larva
import locals.Constants
import maths.RandomNumberGenerator
import grizzled.slf4j.Logging

class LarvaeHistoryCsvFile(larvae: Array[Larva], filepath: String, percent : Double) extends Logging {

  val columnHeaders = "id,born,age,stage,pld,birth_place,state,habitat_id,latitude,longitude,depth"

  def write(): Unit = {
    info("Writing " + larvae.size + " larvae")
    val bw = new BufferedWriter(new java.io.FileWriter(new File(filepath + "//larvae-log.csv")))
    for (larva <- larvae) {
      if(RandomNumberGenerator.getPercent < percent) {
        bw.write(columnHeaders)
        bw.newLine()
        bw.write(buildCsvRow(larva))
      }
    }
    bw.close()
  }

  private def buildCsvRow(larva: Larva): String = {
    val sb = new StringBuilder()
    larva.history.foreach(hist => {
      sb append larva.id + ","
      sb append larva.birthday.toLocalDate.toString + ","
      val age = hist.age.toDouble / Constants.SecondsInDay.toDouble
      sb append f"$age%.2f" + ","
      sb append hist.stage + ","
      val pld = larva.pelagicLarvalDuration.toDouble / Constants.SecondsInDay.toDouble
      sb append f"$pld%.2f" + ","
      sb append larva.birthplace.name + ","
      sb append hist.state + ","
      sb append hist.habitat + ","
      val latitude = hist.position.latitude
      val longitude = hist.position.longitude
      val depth = hist.position.depth
      sb append f"$latitude%.5f" + ","
      sb append f"$longitude%.5f" + ","
      sb append f"$depth%.1f" + ",\n"
    })

    val csvRow = sb.toString().toLowerCase()
    //debug(csvRow)
    csvRow
  }

}

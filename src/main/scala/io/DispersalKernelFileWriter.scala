package io

import java.io.{BufferedWriter, File, FileWriter}

import biology.Larva

class DispersalKernelFileWriter(filename: String, larvae: List[Larva]) {

  /**
   * Writes the dispersal kernel to a CSV file. 
   */
  def writeDispersalKernelToCsv(): Unit = {
    val csvFile = new File(filename)
    val bw = new BufferedWriter(new FileWriter(csvFile))
    bw.write(columnHeaders)
    bw.newLine()
    larvae.find(l => l != null && l.hasSettled).foreach(l => bw.write(getCsvLarvaRow(l)))
    bw.close()
  }

  /**
   * Writes a row for the larva to the CSV file
   * @param larva The larva to write
   *
   *              Prints a row for each larva containing the:
   *              - Id
   *              - Birthday
   *              - Birthplace
   *              - SettlementDate
   *              - Habitat Id
   *              - Habitat Type (reef or other)
   *              - Age
   */
  private def getCsvLarvaRow(larva: Larva): String = {
    val sb = new StringBuilder()
    sb ++= larva.id + ","
    sb ++= larva.birthday + ","
    sb ++= larva.birthplace + ","
    sb ++= larva.settlementDate + ","
    sb ++= larva.polygon.id + ","
    sb ++= larva.polygon.habitat.toString + ","
    sb ++= larva.age + ",\n"
    sb.toString()
  }

  /**
   *
   * @return A csv list of column headers
   */
  private def columnHeaders: String = "id,born,age,birth_place,recruited,reef_id,reef_type,"

}
package io

import java.io.{BufferedWriter, File}

import biology.Larva

class DispersalKernelFile(larvae: Array[Larva], csvFile: File) {

  /**
   * Writes the dispersal kernel to a CSV file.
   */
  def write(): Unit = {
    val bw = new BufferedWriter(new java.io.FileWriter(csvFile))
    bw.write(columnHeaders)
    bw.newLine()
    larvae.filter(larva => larva.isSettled).foreach(settledLarva =>       bw.write(getCsvLarvaRow(settledLarva)))
    bw.close()
  }

  /**
   * Writes a row for the larva to the CSV file
    *
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
    sb ++= larva.birthplace.name + ","
    sb ++= larva.age + ","
    sb ++= larva.settlementDate + ","
    sb ++= larva.polygon + "\n"
    sb.toString()
  }

  /**
   *
   * @return A csv list of column headers
   */
  private def columnHeaders: String = "id,born,birthplace,age,recruited,reef"

}

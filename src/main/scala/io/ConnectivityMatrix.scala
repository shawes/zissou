package io

import java.io.{BufferedWriter, File}

import biology.Larva

class ConnectivityMatrix(larvae: Array[Larva], csvFile: File) {

  def write(): Unit = {
    val bw = new BufferedWriter(new java.io.FileWriter(csvFile))
    bw.write(columnHeaders)
    bw.newLine()
    //val settledLarvae = larvae.filter(larva => larva.isSettled)
    larvae.foreach(larva => bw.write(getCsvLarvaRow(larva)))
    bw.close()
  }

  private def getCsvLarvaRow(larva: Larva): String = {
    val sb = new StringBuilder()
    sb ++= larva.id + ","
    sb ++= larva.birthday.toLocalDate.toString + ","
    sb ++= larva.birthplace.name + ","
    sb ++= larva.birthplace.reef + ","
    sb ++= larva.age + ","
    sb ++= larva.settlementDate.get.toLocalDate.toString + ","
    sb ++= larva.settledHabitatId + "\n"
    sb.toString()
  }

  private def columnHeaders: String =
    "id,born,region,source,age,recruited-date,settle-id"
}

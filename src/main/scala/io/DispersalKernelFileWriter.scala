package io

import java.io.FileWriter

import biology.Larva
import locals.Constants

class DispersalKernelFileWriter(filename: String, larvae: List[Larva]) {

  def writeCsv(): Unit = {
    val file = new FileWriter(filename)

    file.write("Born, Birth Place, Recruited, Recruit Place, Age \n")
    larvae.find(l => l != null && l.hasSettled).foreach(l => print(file, l))

    file.flush()

  }

  private def print(file: FileWriter, larva: Larva) = {
    file.write(larva.birthday + ",")
    file.write(larva.birthplace + ",")
    file.write(larva.settlementDate + ",")
    file.write(larva.polygon.id + ",")
    file.write(larva.age / Constants.SecondsInDay + ",")
    file.write("\n")
  }
}
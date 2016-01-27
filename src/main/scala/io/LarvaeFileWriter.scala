package io

import java.io.{BufferedWriter, File}

import biology.Larva

class LarvaeFileWriter(larvae: List[Larva], file: File) extends FileWriterTrait {

  val columnHeaders = "id,born,age,pld,birth_place,state,habitat_id,habitat_type,latitude,longitude,depth"

  def write(): Unit = {
    val bw = new BufferedWriter(new java.io.FileWriter(file))
    bw.write(columnHeaders)
    bw.newLine()
    larvae.foreach(larvae => bw.write(writeCsvRow(larvae)))
    bw.close()
  }

  private def writeCsvRow(larva: Larva): String = {
    var distance = 0
    val sb = new StringBuilder()
    larva.history.foreach(hist => {
      sb ++= larva.id + ","
      sb ++= larva.birthday + ","
      sb ++= larva.age + ","
      sb ++= larva.pelagicLarvalDuration + ","
      sb ++= larva.birthplace + ","
      sb ++= hist.state + ","
      sb ++= hist.habitat.id + ","
      sb ++= hist.habitat.habitat.toString + ","
      sb ++= hist.position.latitude + ","
      sb ++= hist.position.longitude + ","
      sb ++= hist.position.depth + ",\n"
    })
    sb.toString()

  }

}

package io

import java.io.{BufferedWriter, File}
import biology.Larva
import maths.Geometry

class DispersalKernel(larvae: Array[Larva], csvFile: File) {
  val geometry = new Geometry()

  def write(): Unit = {
    val bw = new BufferedWriter(new java.io.FileWriter(csvFile))
    bw.write(columnHeaders)
    bw.newLine()
    larvae.foreach(larva => bw.write(getCsvLarvaRow(larva)))
    bw.close()
  }

  private def getCsvLarvaRow(larva: Larva): String = {
    val distance = calculateDispersalDistance(larva)
    val sb = new StringBuilder()
    sb ++= larva.id + ","
    sb ++= larva.birthday.toLocalDate.toString + ","
    sb ++= larva.birthplace.name + ","
    sb ++= larva.birthplace.reef + ","
    sb ++= larva.age + ","
    sb ++= larva.polygon + ","
    sb ++= f"$distance%.1f" + "\n"
    sb.toString()
  }

  private def columnHeaders: String = "id,born,birthplace,birth-reef,age,settle-reef,distance"

  private def calculateDispersalDistance(larva: Larva) : Double = {
      //larva.history.foldLeft(0.0)(_+getDistance(_,_))
    var distance = 0.0
    var lastPos = larva.birthplace.location
    larva.history.foreach(t => {
      distance += geometry.getDistanceBetweenTwoPoints(lastPos, t.position)
      lastPos = t.position
    })
    distance
  }

  // private def getDistance(t1 : TimeCapsule, t2 : TimeCapsule) : Double = {
  //   geometry.getDistanceBetweenTwoPoints(t1.position, t2.position)
  // }
}

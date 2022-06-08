package io

import java.io.{BufferedWriter, File}
import biology.Larva
import maths.Geometry
import utilities.Time
import grizzled.slf4j.Logging

class DispersalKernel(larvae: Array[Larva], csvFile: File) extends Logging {
  val geometry = new Geometry()

  def write(): Unit = {
    val bw = new BufferedWriter(new java.io.FileWriter(csvFile))
    bw.write(columnHeaders)
    bw.newLine()
    larvae.foreach(larva => bw.write(getCsvLarvaRow(larva)))
    bw.close()
  }

  private def getCsvLarvaRow(larva: Larva): String = {

    val sb = new StringBuilder()
    sb ++= larva.id + ","
    sb ++= larva.birthday.toLocalDate.toString + ","
    sb ++= larva.birthplace.name + ","
    sb ++= larva.birthplace.reef.toString() + ","
    val age = Time.convertSecondsToDays(larva.age)
    sb ++= f"$age%.1f" + ","
    sb ++= larva.settledHabitatId.toString() + ","
    val dispersalDistance = calculateDispersalDistance(larva)
    sb ++= f"$dispersalDistance%.1f" + ","
    val crowFliesDistance = calculateCrowFliesDistance(larva)
    sb ++= f"$crowFliesDistance%.1f" + "\n"
    sb.toString()
  }

  private def columnHeaders: String =
    "id,born,birthplace,birth-reef,age,settle-reef,dispersal-distance,crow-flies-distance"

  private def calculateDispersalDistance(larva: Larva): Double = {
    var distance: Double = 0.0
    var lastPos = larva.birthplace.location
    if (larva.history.size > 1) then {
      larva.history.foreach(t => {
        distance += geometry.getDistanceBetweenTwoPoints(lastPos, t.position)
        lastPos = t.position
      })
    }
    distance
  }

  private def calculateCrowFliesDistance(larva: Larva): Double = {
    larva.history.size match {
      case 0 => 0
      case _ =>
        geometry.getDistanceBetweenTwoPoints(
          larva.birthplace.location,
          larva.history.last.position
        )
    }
  }

}

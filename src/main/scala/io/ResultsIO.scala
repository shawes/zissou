package io

import java.io.File

import biology.Larva

class ResultsIO(larvae: Array[Larva], output: OutputFiles, name : String) {

  val directory = new File(output.path + "//" + name)
  directory.mkdir()
  val shapeFile = new File(directory.toPath + "//larvae-dispersal.shp")
  val kernelFile = new File(directory.toPath + "//connectivity-matrix.csv")

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    writeConnectivityMatrix()
    if (output.includeLarvaeMovements) writeLarvaeStateChangesToCsvFile()
  }

  private def writeLarvaeStateChangesToCsvFile() = {
    val larvaeFileWriter = new LarvaeHistoryCsvFile(larvae, directory.toPath.toString, output.percent)
    larvaeFileWriter.write()
  }

  private def writeConnectivityMatrix() = {
    val connectivityMatrix = new ConnectivityMatrix(larvae, kernelFile)
    connectivityMatrix.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new GisShapeFile()
    shapeFileWriter.write(larvae, output.shapeType, shapeFile, output.percent)
  }
}

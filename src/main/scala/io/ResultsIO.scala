package io

import java.io.File

import biology.Larva

class ResultsIO(larvae: Array[Larva], output: OutputFiles, name : String) {

  val directory = new File(output.path + "//" + name)
  directory.mkdir()
  val shapeFileAll = new File(directory.toPath + "//" + output.prefix + "-dispersal.shp")
  val shapeFileSettled = new File(directory.toPath + "//" + output.prefix + "-settled.shp")
  val connectivityMatrixFile = new File(directory.toPath + "//" + output.prefix + "-connectivity-matrix.csv")
  val dispersalKernelFile = new File(directory.toPath + "//" + output.prefix + "-dispersal-kernel.csv")
  val settledLarvae = larvae.filter(larva => larva.isSettled)

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    writeConnectivityMatrix()
    writeDispersalKernel()
    if (output.includeLarvaeMovements) writeLarvaeStateChangesToCsvFile()
  }

  private def writeLarvaeStateChangesToCsvFile() = {
    val larvaeFileWriter = new LarvaeHistoryCsvFile(larvae, directory.toPath.toString, output.percent)
    larvaeFileWriter.write()
  }

  private def writeConnectivityMatrix() = {
    val connectivityMatrix = new ConnectivityMatrix(settledLarvae, connectivityMatrixFile)
    connectivityMatrix.write()
  }

  private def writeDispersalKernel() = {
    val dispersalKernel = new DispersalKernel(settledLarvae, dispersalKernelFile)
    dispersalKernel.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new GisShapeFile()
    shapeFileWriter.write(larvae, shapeFileAll, output.percent)
    shapeFileWriter.write(settledLarvae, shapeFileSettled, 100)
  }
}

package io

import java.io.File

import biology.Larva

class ResultsIO(larvae: Array[Larva], output: OutputFiles, name : String) {

  val directory = new File(output.path + "//" + name)
  directory.mkdir()
  val shapeFile = new File(directory.toPath + "//"+output.prefix+"-dispersal.shp")
  val connectivityMatrixFile = new File(directory.toPath + "//"+output.prefix+"-connectivity-matrix.csv")
  val dispersalKernelFile = new File(directory.toPath + "//"+output.prefix+"-dispersal-kernel.csv")

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
    val connectivityMatrix = new ConnectivityMatrix(larvae, connectivityMatrixFile)
    connectivityMatrix.write()
  }

  private def writeDispersalKernel() = {
    val dispersalKernel = new DispersalKernel(larvae, dispersalKernelFile)
    dispersalKernel.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new GisShapeFile()
    shapeFileWriter.write(larvae, shapeFile, output.percent)
  }
}

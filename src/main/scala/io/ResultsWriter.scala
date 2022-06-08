package io

import java.io.File
import grizzled.slf4j.Logging
import biology._

class ResultsWriter(larvae: Array[Larva], output: OutputFiles, name: String)
    extends Logging {

  val directory = new File(output.path + "//" + name)
  directory.mkdir()
  val shapeFileAll = new File(
    directory.toPath.toString() + "//" + output.prefix + "-dispersal.shp"
  )
  val shapeFileSettled = new File(
    directory.toPath.toString() + "//" + output.prefix + "-settled.shp"
  )
  val connectivityMatrixFile = new File(
    directory.toPath
      .toString() + "//" + output.prefix + "-connectivity-matrix.csv"
  )
  val dispersalKernelFile = new File(
    directory.toPath.toString() + "//" + output.prefix + "-dispersal-kernel.csv"
  )
  val settledLarvae = larvae.filter(larva => larva.isSettled)

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    info("Finished writing shape files")
    writeConnectivityMatrix()
    info("Finished writing connectivity matrix")
    writeDispersalKernel()
    info("Finished writing dispersal kernel")
    if (output.includeLarvaeMovements) then {
      writeLarvaeHistoryToCsvFile()
      info("Finished writing larvae history")
    }
  }

  private def writeLarvaeHistoryToCsvFile() = {
    val larvaeFileWriter = new LarvaeHistoryCsvFile(
      larvae,
      directory.toPath.toString,
      output.percent
    )
    larvaeFileWriter.write()
  }

  private def writeConnectivityMatrix() = {
    val connectivityMatrix =
      new ConnectivityMatrix(settledLarvae, connectivityMatrixFile)
    connectivityMatrix.write()
  }

  private def writeDispersalKernel() = {
    val dispersalKernel =
      new DispersalKernel(settledLarvae, dispersalKernelFile)
    dispersalKernel.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new GisShapeFile()
    shapeFileWriter.write(larvae, shapeFileAll, output.percent)
    shapeFileWriter.write(settledLarvae, shapeFileSettled, 100)
  }
}

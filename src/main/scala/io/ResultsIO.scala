package io

import java.io.File

import biology.Larva

/**
  * Created by steve on 26/01/2016.
  */
class ResultsIO(larvae: List[List[Larva]], output: OutputFiles) {

  val shapeFile = new File(output.path + "//larvae_paths.shp")
  val kernelFile = new File(output.path + "//dispersal_kernel.csv")
  val larvalPathsFile = new File(output.path + "//larvae_paths.csv")

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    writeDispersalKernel()
    if (output.includeLarvaeMovements) writeLarvaeStateChangesToCsvFile()
  }


  private def writeLarvaeStateChangesToCsvFile() = {
    val larvaeFileWriter = new LarvaeHistoryCsvFile(larvae, output.path)
    larvaeFileWriter.write()
    //larvaeFileWriter.writeExcelFile(fishLarvae.flatten, output.saveOutputFilePath)
  }

  private def writeDispersalKernel() = {
    val dispersalKernelWriter = new DispersalKernelFile(larvae.flatten, kernelFile)
    dispersalKernelWriter.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new GisShapeFile()
    shapeFileWriter.write(larvae.flatten, output.shapeType, shapeFile)

    //shapeFileWriter.writeShapes(larvae, output.SaveOutputFilePath, output.ShapeType);
  }
}

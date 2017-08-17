package io

import java.io.File

import biology.Larva

class ResultsIO(larvae: Array[Larva], output: OutputFiles, name : String) {

  val directory = new File(output.path + "//" + name)
  directory.mkdir()
  val shapeFile = new File(directory.toPath + "//larvae-dispersal.shp")
  val kernelFile = new File(directory.toPath + "//dispersal-kernel.csv")
  //val larvalPathsFile = new File(directory.toPath + "//larvae_paths.csv")

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    writeDispersalKernel()
    if (output.includeLarvaeMovements) writeLarvaeStateChangesToCsvFile()
  }

  private def writeLarvaeStateChangesToCsvFile() = {
    val larvaeFileWriter = new LarvaeHistoryCsvFile(larvae, directory.toPath.toString)
    larvaeFileWriter.write()
    //larvaeFileWriter.writeExcelFile(fishLarvae.flatten, output.saveOutputFilePath)
  }

  private def writeDispersalKernel() = {
    val dispersalKernelWriter = new DispersalKernelFile(larvae, kernelFile)
    dispersalKernelWriter.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new GisShapeFile()
    shapeFileWriter.write(larvae, output.shapeType, shapeFile, output.percent)

    //shapeFileWriter.writeShapes(larvae, output.SaveOutputFilePath, output.ShapeType);
  }
}

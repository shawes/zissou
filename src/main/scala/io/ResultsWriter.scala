package io

import java.io.File

import biology.ReefFish

/**
  * Created by steve on 26/01/2016.
  */
class ResultsWriter(larvae: List[ReefFish], output: OutputFiles) extends FileWriterTrait {

  val shapeFile = new File(output.path + "//larvae_paths.shp")
  val kernelFile = new File(output.path + "//dispersal_kernel.csv")
  val larvalPathsFile = new File(output.path + "//larvae_paths.csv")

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    writeDispersalKernel()
    if (output.includeLarvaeMovements) writeLarvaeStateChangesToCsvFile()
  }


  private def writeLarvaeStateChangesToCsvFile() = {
    val larvaeFileWriter = new LarvaeFileWriter(larvae, larvalPathsFile)
    larvaeFileWriter.write()
    //larvaeFileWriter.writeExcelFile(fishLarvae.flatten, output.saveOutputFilePath)
  }

  private def writeDispersalKernel() = {
    val dispersalKernelWriter = new DispersalKernelFileWriter(larvae, kernelFile)
    dispersalKernelWriter.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new ShapeFileWriter(larvae, output.shapeType, shapeFile)
    shapeFileWriter.write()

    //shapeFileWriter.writeShapes(larvae, output.SaveOutputFilePath, output.ShapeType);
  }
}

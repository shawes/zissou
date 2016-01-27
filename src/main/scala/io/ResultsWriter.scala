package io

import java.io.File

import biology.ReefFish
import locals.ShapeFileType

/**
  * Created by steve on 26/01/2016.
  */
class ResultsWriter(larvae: List[ReefFish], output: OutputFiles) extends FileWriterTrait {

  val file = new File(output.path)

  def write(): Unit = {
    writeLarvaeMovementsToShapeFile()
    writeDispersalKernel()
    if (output.includeLarvaeMovements) writeLarvaeStateChangesToCsvFile()
  }


  private def writeLarvaeStateChangesToCsvFile() = {
    val larvaeFileWriter = new LarvaeFileWriter(larvae, file)
    larvaeFileWriter.write()
    //larvaeFileWriter.writeExcelFile(fishLarvae.flatten, output.saveOutputFilePath)
  }

  private def writeDispersalKernel() = {
    val dispersalKernelWriter = new DispersalKernelFileWriter(larvae, file)
    dispersalKernelWriter.write()
  }

  private def writeLarvaeMovementsToShapeFile() = {
    val shapeFileWriter = new ShapeFileWriter(larvae, ShapeFileType.Line, file) //TODO: Remove hard coded shape file type
    shapeFileWriter.write()

    //shapeFileWriter.writeShapes(larvae, output.SaveOutputFilePath, output.ShapeType);
  }
}

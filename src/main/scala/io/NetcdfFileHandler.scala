package io

import java.io.IOException

import grizzled.slf4j.Logging
import ucar.nc2.dt.grid.GridDataset

class NetcdfFileHandler extends Logging {

  def openLocalFile(file: String): GridDataset = {
    var dataset: GridDataset = null
    try {
      debug("Reading the file" + file)
      GridDataset.open(file)

      //gridFile
    } catch {
      case ioe: IOException => error("Error opening the " + file + ", getting exception: " + ioe.getMessage)
    }
    dataset
  }
}

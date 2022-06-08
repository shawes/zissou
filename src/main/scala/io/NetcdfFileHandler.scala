package io

import grizzled.slf4j.Logging
import ucar.nc2.dt.grid.GridDataset
import ucar.nc2.dataset.NetcdfDatasets

class NetcdfFileHandler extends Logging {

  def openLocalFile(file: String): GridDataset = GridDataset.open(file)

  def shutdown(): Unit = NetcdfDatasets.shutdown()

}

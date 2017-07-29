package io

import grizzled.slf4j.Logging
import ucar.nc2.dt.grid.GridDataset
import ucar.nc2.dataset.NetcdfDataset

class NetcdfFileHandler extends Logging {

  //NetcdfDataset.initNetcdfFileCache(1,200,15*60)

  def openLocalFile(file: String): GridDataset = {
     GridDataset.open(file)
  }

  def shutdown() : Unit = {
    NetcdfDataset.shutdown()
  }
}

package io

import java.io.File

import org.geotools.data.FileDataStoreFinder
import org.geotools.data.simple.SimpleFeatureCollection

/**
  * Created by steve on 25/01/2016.
  */
class HabitatFileReader() {

  def read(file : File): SimpleFeatureCollection = {
    val store = FileDataStoreFinder.getDataStore(file)
    try {
      store.getFeatureSource.getFeatures
    } finally {
      store.dispose()
    }
  }
}

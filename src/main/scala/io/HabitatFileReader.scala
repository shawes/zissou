package io

import java.io.File

import org.geotools.data.{DataUtilities, FileDataStoreFinder}
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.feature.FeatureCollection

/**
  * Created by steve on 25/01/2016.
  */
class HabitatFileReader() extends FileReaderTrait {

  def read(file : File): SimpleFeatureCollection = {
    val store = FileDataStoreFinder.getDataStore(file)
    try {
      val features = store.getFeatureSource.getFeatures
      DataUtilities.collection(features)
    } finally {
      store.dispose()
    }
  }
}

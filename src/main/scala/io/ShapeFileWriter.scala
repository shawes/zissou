package io

import java.io.{File, Serializable}
import java.util

import biology.{Larva, TimeCapsule}
import com.vividsolutions.jts.geom.Point
import locals.ShapeFileType._
import locals.{PelagicLarvaeState, ShapeFileType}
import org.geotools.data.DefaultTransaction
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.shapefile.{ShapefileDataStore, ShapefileDataStoreFactory}
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

class ShapeFileWriter(larvae: List[Larva], shape: ShapeFileType) {

  val featureBuilder = new SimpleFeatureBuilder(createPointSchema())
  val features = new java.util.ArrayList[SimpleFeature]

  def write(file: File) = {
    require(larvae != null && shape != null)

    if (shape == ShapeFileType.Line) writeLineShapeFile() else writePointShapeFile(file) // Will need to expand if more options

  }

  private def writePointShapeFile(file: File) = {
    features.clear()
    larvae.foreach(l => l.history.foreach(hist => addFeature(l.id, hist)))

    val dataStoreFactory = new ShapefileDataStoreFactory()

    val params = createParams(file)

    val newDataStore = dataStoreFactory.createNewDataStore(params).asInstanceOf[ShapefileDataStore]
    newDataStore.createSchema(createPointSchema())

    val transaction = new DefaultTransaction("create")

    val typeName: String = newDataStore.getTypeNames()(0)
    val featureSource = newDataStore.getFeatureSource(typeName)

    featureSource match {
      case sfs: SimpleFeatureStore =>
        val featureStore = featureSource.asInstanceOf[SimpleFeatureStore]
        val collection = new ListFeatureCollection(createPointSchema(), features)
        featureStore.setTransaction(transaction)
        featureStore.addFeatures(collection)
        transaction.commit()
        transaction.close()
      case _ =>
        throw new IllegalArgumentException()
    }
  }

  private def createParams(file: File): util.HashMap[String, Serializable] = {
    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    params.put("create spatial index", true)
    params
  }

  private def addFeature(id: Int, time: TimeCapsule): Unit = {
    featureBuilder.add(id)
    featureBuilder.add(time.position)
    featureBuilder.add(time.state == PelagicLarvaeState.Settled)
    features.add(featureBuilder.buildFeature(null))
  }

  private def createPointSchema(): SimpleFeatureType = {
    val schema = new SimpleFeatureTypeBuilder()
    schema.setName("Location")
    schema.setCRS(DefaultGeographicCRS.WGS84)
    schema.add("id", Int.getClass)
    schema.add("location", classOf[Point])
    schema.add("settled", Boolean.getClass)
    schema.buildFeatureType()
  }

  private def writeLineShapeFile() = {
    //larvae.foreach()
  }

  private def ShapeFileName = "LarvaePaths.shp"

}

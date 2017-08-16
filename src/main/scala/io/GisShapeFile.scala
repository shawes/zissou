package io

import java.io.{File, Serializable}
import java.util

import biology.{Larva, TimeCapsule}
import com.vividsolutions.jts.geom.{Coordinate, LineString, Point}
import maths.RandomNumberGenerator

import locals.ShapeFileType._
import locals.ShapeFileType
import org.geotools.data._
import grizzled.slf4j.Logging

import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.{SimpleFeatureCollection, SimpleFeatureStore}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter.Filter
import org.geotools.factory.CommonFactoryFinder
import physical.adaptors.GeometryToGeoCoordinateAdaptor
import scala.collection.JavaConverters._

import scala.collection.mutable.ListBuffer

class GisShapeFile() extends Logging {

  val geometryFactory = JTSFactoryFinder.getGeometryFactory()

  def read(file: File): ListFeatureCollection = {
    val store = FileDataStoreFinder.getDataStore(file)
    try {
      val ff = CommonFactoryFinder.getFilterFactory2()
      val filters : ListBuffer[Filter] = ListBuffer.empty
      filters += ff.equals(ff.property("HABITAT"), ff.literal("Reef"))
      filters += ff.equals(ff.property("HABITAT"), ff.literal("Other"))
      val features = store.getFeatureSource.getFeatures(ff.or(filters.asJava))
      new ListFeatureCollection(DataUtilities.collection(features))
    } finally {
      store.dispose()
    }
  }

  def write(larvae: List[Larva], shape: ShapeFileType, file: File, percent : Double): Unit = shape match {
    case ShapeFileType.Line => writeLineShapeFile(larvae, file, percent)
    case ShapeFileType.Point => writePointShapeFile(larvae, file, percent)
    case _ => throw new scala.IllegalArgumentException()
  }

  private def writeLineShapeFile(larvae: List[Larva], file: File, percent : Double) = {
    val features = new java.util.ArrayList[SimpleFeature]
    val builder :SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder()
    builder.setName("larva")
    builder.setCRS(DefaultGeographicCRS.WGS84) // <- Coordinate reference system
    builder.add("the_geom", classOf[LineString])
    builder.add("birth", classOf[String])
    builder.add("settle",classOf[Integer])


    val  larvaLine:SimpleFeatureType = builder.buildFeatureType();

    for (larva <- larvae) {
      if(larva.history.size > 1 && RandomNumberGenerator.get*100 < percent) {
        val featureBuilder = new SimpleFeatureBuilder(larvaLine)
        featureBuilder.add(writeStageLine(larva.history.toList))
        featureBuilder.add(larva.birthplace)
        if(larva.polygon.isDefined) {
          featureBuilder.add(larva.polygon)
        } else {
          featureBuilder.add(-1)
        }
        val feature = featureBuilder.buildFeature(null)
        features.add(feature)
     }
    }

    val dataStoreFactory: FileDataStoreFactorySpi = new ShapefileDataStoreFactory()
    val params = createParams(file)
    val newDataStore = dataStoreFactory.createNewDataStore(params)
    newDataStore.createSchema(larvaLine)
    val collection = new ListFeatureCollection(larvaLine, features)
    val typeName: String = newDataStore.getTypeNames.head
    val featureSource = newDataStore.getFeatureSource(typeName)
    val createTransaction = new DefaultTransaction("create")
    val featureStore = featureSource.asInstanceOf[SimpleFeatureStore]
    featureStore.setTransaction(createTransaction)
    featureStore.addFeatures(collection)
    createTransaction.commit()
    createTransaction.close()
  }

  private def writeStageLine(history: List[TimeCapsule]): LineString = {
///val coordinates = new ListBuffer[Coordinate]()
    ////if(history.size > 1) {
    val coordinates = new ListBuffer[Coordinate]()
    for (hist <- history) {
      //TODO: Remove repeating points
      coordinates += new Coordinate(hist.position.longitude, hist.position.latitude)
    }
    geometryFactory.createLineString(coordinates.toArray)
  //}
  }

  private def createParams(file: File): util.HashMap[String, Serializable] = {
    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    params.put("create spatial index", true)
    params
  }

  private def createLineSchema(): SimpleFeatureType = {
    val schema = new SimpleFeatureTypeBuilder()
    schema.setName("LarvaePaths")
    schema.setCRS(DefaultGeographicCRS.WGS84)
    schema.add("Path", classOf[LineString])
    schema.add("Id", classOf[Integer])
    schema.buildFeatureType()
  }

  private def writePointShapeFile(larvae: List[Larva], file: File, percent : Double) = {
    val features = new java.util.ArrayList[SimpleFeature]
    val featureBuilder = new SimpleFeatureBuilder(createPointSchema())
    larvae.foreach(l => l.history.foreach(hist => addPointFeature(featureBuilder, l.id, hist)))
    val dataStoreFactory: FileDataStoreFactorySpi = new ShapefileDataStoreFactory()

    val params = createParams(file)
    val newDataStore = dataStoreFactory.createNewDataStore(params)
    newDataStore.createSchema(createPointSchema())
    writeFeaturesToShapeFile(features, newDataStore)
  }

  private def writeFeaturesToShapeFile(features: util.ArrayList[SimpleFeature], dataStore: DataStore): Unit = {
    val featureType = createLineSchema()
    val collection = new ListFeatureCollection(featureType, features)

    val transaction = new DefaultTransaction("create")
    val typeName: String = dataStore.getTypeNames()(0)
    val featureSource = dataStore.getFeatureSource(typeName)

    val featureStore = featureSource.asInstanceOf[SimpleFeatureStore]
    featureStore.setTransaction(transaction)
    featureStore.addFeatures(collection)
    transaction.commit()
    transaction.close()
  }

  private def addPointFeature(featureBuilder: SimpleFeatureBuilder, id: Int, time: TimeCapsule): SimpleFeature = {
    featureBuilder.add(id)
    featureBuilder.add(GeometryToGeoCoordinateAdaptor.toPoint(time.position))
    //featureBuilder.add(time.state == PelagicLarvaeState.Settled)
    featureBuilder.buildFeature(null)
  }

  private def createPointSchema(): SimpleFeatureType = {
    val schema = new SimpleFeatureTypeBuilder()
    schema.setName("LarvaePaths")
    schema.setCRS(DefaultGeographicCRS.WGS84)
    schema.add("the_geom", classOf[Point])
    schema.add("id", classOf[Integer])
    schema.buildFeatureType()
  }



}

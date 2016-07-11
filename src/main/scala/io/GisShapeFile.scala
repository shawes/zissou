package io

import java.io.{File, Serializable}
import java.util

import biology.{Larva, TimeCapsule}
import com.vividsolutions.jts.geom.{Coordinate, LineString, MultiLineString, Point}
import locals.ShapeFileType._
import locals.{OntogenyState, ShapeFileType}
import org.geotools.data.{DataStore, DefaultTransaction, FileDataStoreFactorySpi}

//import org.geotools.data.{DataStore, DefaultTransaction}
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import physical.adaptors.GeometryToGeoCoordinateAdaptor

import scala.collection.mutable.ListBuffer

class GisShapeFile(larvae: List[Larva], shape: ShapeFileType, file: File) {

  val ShapeFileName = "LarvaePaths.shp"
  val geometryFactory = JTSFactoryFinder.getGeometryFactory()

  def write(): Unit = shape match {
    case ShapeFileType.Line => writeLineShapeFile(file)
    case ShapeFileType.Point => writePointShapeFile(file)
    case _ => throw new scala.IllegalArgumentException()
  }

  private def writeLineShapeFile(file: File) = {

    val features = new java.util.ArrayList[SimpleFeature]
    val featureBuilder = new SimpleFeatureBuilder(createLineSchema())

    for (larva <- larvae) {
      val hatchingFilter = larva.history.filter(x => x.stage == OntogenyState.Hatching)
      val preflexionFilter = larva.history.filter(x => x.stage == OntogenyState.Preflexion)
      val flexionFilter = larva.history.filter(x => x.stage == OntogenyState.Flexion)
      val postFlexionFilter = larva.history.filter(x => x.stage == OntogenyState.Postflexion)

      val lines = new ListBuffer[LineString]()
      lines += writeStageLine(hatchingFilter.toList)
      lines += writeStageLine(preflexionFilter.toList)
      lines += writeStageLine(flexionFilter.toList)
      lines += writeStageLine(postFlexionFilter.toList)

      addMultiLineFeature(featureBuilder, larva.id, lines.toArray)
    }
    val dataStoreFactory: FileDataStoreFactorySpi = new ShapefileDataStoreFactory()
    val params = createParams(file)
    val featureType = createLineSchema()
    val newDataStore = dataStoreFactory.createNewDataStore(params)
    newDataStore.createSchema(featureType)
    //writeFeaturesToShapeFile(features, newDataStore)
    val collection = new ListFeatureCollection(featureType, features)
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
    val coordinates = new ListBuffer[Coordinate]()
    for (hist <- history) {
      coordinates += new Coordinate(hist.position.latitude, hist.position.longitude)
    }
    if (coordinates.size > 1) {
      geometryFactory.createLineString(coordinates.toArray)
    } else {
      geometryFactory.createLineString(new Array[Coordinate](0))
    }
  }

  private def createParams(file: File): util.HashMap[String, Serializable] = {
    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    params.put("create spatial index", true)
    params
  }

  private def addMultiLineFeature(featureBuilder: SimpleFeatureBuilder, id: Int, lines: Array[LineString]): SimpleFeature = {

    val multi = geometryFactory.createMultiLineString(lines)
    featureBuilder.add(id)
    featureBuilder.add(multi)
    featureBuilder.buildFeature(null)
  }

  private def createLineSchema(): SimpleFeatureType = {
    val schema = new SimpleFeatureTypeBuilder()
    schema.setName("LarvaePaths")
    schema.setCRS(DefaultGeographicCRS.WGS84)
    schema.add("the_geom", classOf[MultiLineString])
    schema.add("id", classOf[Integer])
    schema.buildFeatureType()
  }

  private def writePointShapeFile(file: File) = {
    val features = new java.util.ArrayList[SimpleFeature]
    val featureBuilder = new SimpleFeatureBuilder(createPointSchema())
    larvae.foreach(l => l.history.foreach(hist => addPointFeature(featureBuilder, l.id, hist)))
    val dataStoreFactory: FileDataStoreFactorySpi = new ShapefileDataStoreFactory()

    val params = createParams(file)
    val newDataStore = dataStoreFactory.createNewDataStore(params)
    // val newShapeFileDataStore : ShapefileDataStore = newDataStore
    newDataStore.createSchema(createPointSchema())
    writeFeaturesToShapeFile(features, newDataStore)
  }

  private def writeFeaturesToShapeFile(features: util.ArrayList[SimpleFeature], dataStore: DataStore): Unit = {
    val featureType = createLineSchema()
    val collection = new ListFeatureCollection(featureType, features)




    val transaction = new DefaultTransaction("create")
    val typeName: String = dataStore.getTypeNames()(0)
    val featureSource = dataStore.getFeatureSource(typeName)


    /* featureSource match {
       case sfs: SimpleFeatureStore =>
         val featureStore = featureSource.asInstanceOf[SimpleFeatureStore]
         if (shape == ShapeFileType.Line) {
         val collection = new ListFeatureCollection(createLineSchema(), features)
         featureStore.setTransaction(transaction)
         featureStore.addFeatures(collection)
         transaction.commit()
           transaction.close()
         }
         else {
           val collection = new ListFeatureCollection(createLineSchema(), features)
           featureStore.setTransaction(transaction)
           featureStore.addFeatures(collection)
           transaction.commit()
         transaction.close()
         }
       case _ =>
         throw new scala.IllegalArgumentException()
     }*/
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

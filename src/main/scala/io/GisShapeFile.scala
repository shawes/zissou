package io

import java.io.{File, Serializable}
import java.net.URL
import java.util
import biology.{Larva, TimeCapsule}
import org.locationtech.jts.geom.{Coordinate, LineString}
import maths.RandomNumberGenerator
import org.geotools.data._
import grizzled.slf4j.Logging
import org.geotools.data.simple._
import org.geotools.data.collection._
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.data.shapefile._
import org.locationtech.jts.{geom => jts}
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.geometry.jts.JTSFactoryFinder
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class GisShapeFile() extends Logging {

  val geometryFactory = JTSFactoryFinder.getGeometryFactory()

  def read(path: String): Seq[SimpleFeature] = {
    val file = new File(path)
    read(file.toURI.toURL)
    // val store = FileDataStoreFinder.getDataStore(file)
    // if (store == null) {
    //   info("There is no store")
    // }
    // try {
    //   val ff = CommonFactoryFinder.getFilterFactory2()
    //   val filters: ListBuffer[Filter] = ListBuffer.empty
    //   filters += ff.equals(ff.property("HABITAT"), ff.literal("Reef"))
    //   filters += ff.equals(
    //     ff.property("HABITAT"),
    //     ff.literal("Rocky intertidal")
    //   )
    //   filters += ff.equals(ff.property("HABITAT"), ff.literal("Other"))
    //   val features = store.getFeatureSource.getFeatures(ff.or(filters.asJava))
    //   new ListFeatureCollection(DataUtilities.collection(features))
    // } finally {
    //   store.dispose()
    // }
  }

  def read(url: URL): List[SimpleFeature] = {
    // Extract the features as GeoTools 'SimpleFeatures'

    /*      File file = new File("example.shp");
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.dataStore(filter);
     */
    //val file = new File(path)
    //info("is file there: " + (file == null))
    val shapeFile = new ShapefileDataStore(url)
    //info("is shapefile there: " + (shapeFile == null))
    val features = shapeFile.getFeatureSource().getFeatures().features()
    try {
      val simpleFeatures = mutable.ListBuffer[SimpleFeature]()
      while (features.hasNext) simpleFeatures += features.next()
      simpleFeatures.toList
    } finally {
      features.close
      shapeFile.dispose
    }
  }

  def write(larvae: Array[Larva], file: File, percent: Double): Unit = {
    writeLineShapeFile(larvae, file, percent)
  }

  private def writeLineShapeFile(
      larvae: Array[Larva],
      file: File,
      percent: Double
  ) = {
    //info("Writing shape file")
    val features = new java.util.ArrayList[SimpleFeature]
    val builder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder()
    builder.setName("larva")
    builder.setCRS(DefaultGeographicCRS.WGS84) // <- Coordinate reference system
    builder.add("the_geom", classOf[LineString])
    //builder.add("birth", classOf[String])
    //builder.add("settle",classOf[Integer])

    val larvaLine: SimpleFeatureType = builder.buildFeatureType()

    for (larva <- larvae) {
      if (larva.history.size > 1 && RandomNumberGenerator.getPercent < percent) {
        val featureBuilder = new SimpleFeatureBuilder(larvaLine)
        featureBuilder.add(writeStageLine(larva.history.toArray))
        //featureBuilder.add(larva.birthplace)
        //featureBuilder.add(larva.polygon)
        val feature = featureBuilder.buildFeature(null)
        features.add(feature)
      }
    }

    val dataStoreFactory: FileDataStoreFactorySpi =
      new ShapefileDataStoreFactory()
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
    try {
      createTransaction.commit()
    } catch {
      case ex: Throwable =>
        error("Error writing the shape file: " + ex.printStackTrace)
    } finally {
      createTransaction.close()
    }

  }

  private def writeStageLine(history: Array[TimeCapsule]): LineString = {
    val coordinates = new ListBuffer[Coordinate]()
    for (hist <- history) {
      coordinates += new Coordinate(
        hist.position.longitude,
        hist.position.latitude
      )
    }
    geometryFactory.createLineString(coordinates.toArray)
  }

  private def createParams(file: File): util.HashMap[String, Serializable] = {
    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    params.put("create spatial index", true)
    params
  }

}

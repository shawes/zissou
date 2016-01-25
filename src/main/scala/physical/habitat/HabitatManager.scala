package physical.habitat

import java.io.File
import java.util

import com.vividsolutions.jts.geom.Point
import grizzled.slf4j.Logger
import io.HabitatFileReader
import locals.Constants
import org.geotools.data.FileDataStoreFinder
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.factory.{CommonFactoryFinder, GeoTools}
import org.opengis.feature.simple.SimpleFeature
import org.opengis.filter.Filter
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor

import scala.collection.mutable


class HabitatManager(file: File, buffer: Buffer, habitatTypes: Array[String]) {

  val habitatReader = new HabitatFileReader()
  val habitats: SimpleFeatureCollection = habitatReader.read(file)
  val filteredHabitats: SimpleFeatureCollection = filterHabitats
  //private val habitatsHashTable = new collection.mutable.HashMap[Int, HabitatPolygon]
  val logger = Logger(classOf[HabitatManager])
  private val habitatsHashTable = defineHashTable()
  var habitatList: util.ArrayList[HabitatPolygon] = new util.ArrayList()



  /*
  def loadHabitats(file: File, habitatTypes: Array[String]) : Unit = {
    val store = FileDataStoreFinder.getDataStore(file)


    val featureSource = store.getFeatureSource

    //val collection = featureSource.getFeatures() //simple feature collection
    //println("There are "+collection.size()+" polygons in total")
    //filterHabitats(collection, Array("Reef", "Other"))
    filterHabitats(habitatTypes)
    //println("There are "+habitatFiltered.size()+" reef and other polygons")
    //filteredHabitats
    val shapes = filteredHabitats.features() //simple feature iterator
    val shape = shapes.next()
    val geometry = shape.getAttribute(Constants.ShapeAttribute.Geometry._1).asInstanceOf[Geometry]

    val attributes = shape.getAttributes
    val multi: MultiPolygon = null




    println("Scored some features")

  }
  */

  /*
  This method find the closest reef to the point returns null otherwise
   */
  //TODO: Uses brute-force algorithm, need to change to divide and conquer
  def getClosestHabitat(coordinate: GeoCoordinate): SimpleFeature = {

    val location: Point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)
    var shortestDistance: Double = Double.MaxValue
    var closestReef: SimpleFeature = null
    val shapes = filteredHabitats.features()
    while (shapes.hasNext) {
      val shape = shapes.next()
      val geometry = SimpleFeatureAdaptor.getGeometry(shape)
      val distance = geometry.distance(location)
      if (distance < shortestDistance) {
        shortestDistance = distance
        closestReef = shape
      }
    }
    closestReef
  }

  def getReef(index: Int): HabitatPolygon = habitatsHashTable.get(index).get

  def isCoordinateOverReef(coordinate: GeoCoordinate): Int = {
    logger.debug("Entering isCoordinateOverReef")
    val shapes = filteredHabitats.features()
    val location: Point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)
    while (shapes.hasNext) {
      val shape = shapes.next
      val geometry = SimpleFeatureAdaptor.getGeometry(shape)
      if (geometry.contains(location)) return SimpleFeatureAdaptor.getId(shape)
    }
    Constants.NoClosestReefFound
  }

  private def filterHabitats: SimpleFeatureCollection = {
    //val filterFactory = CQL.toFilter("HABITAT = 'REEF'")
    //val filter  = filterFactory.property
    val filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints)
    val filterList: util.List[Filter] = new util.ArrayList[Filter]()
    habitatTypes.foreach(x => filterList.add(filterFactory.equals(filterFactory.property(Constants.ShapeAttribute.Habitat._2), filterFactory.literal(x))))


    //reefFilter.add(ff.equals(ff.property("HABITAT"),ff.literal(HabitatType.Reef)))
    //reefFilter.add(ff.equals(ff.property("HABITAT"),ff.literal(HabitatType.Other)))

    val filter = filterFactory.or(filterList)

    //PropertyName propertyName = ff.property( "testString" );
    //Literal literal = ff.literal( 2 );
    //PropertyIsEqualTo filter = ff.equals( propertyName, literal );
    habitats.subCollection(filter)
  }

  private def defineHashTable(): mutable.HashMap[Int, HabitatPolygon] = {

    val hash = new collection.mutable.HashMap[Int, HabitatPolygon]
    val shapes = filteredHabitats.features()
    try {
      while (shapes.hasNext) {
        val shape = shapes.next()
        val geometry = SimpleFeatureAdaptor.getGeometry(shape)
        val id: Int = SimpleFeatureAdaptor.getId(shape)
        val habitatType = SimpleFeatureAdaptor.getHabitatType(shape)
        val habitatPolygon = new GeometryAdaptor(geometry, id, habitatType) with HabitatPolygonToJtsGeometryAdaptor with HabitatPolygon
        hash.put(id, habitatPolygon)
        habitatList.add(habitatPolygon.asInstanceOf[HabitatPolygon])
      }
    } finally {
      shapes.close()
    }
    hash
  }


  //    private int GetIndexOfReef(GeoCoordinate c)
  //    {
  //      var coordinate = new Coordinate(c.Longitude, c.Latitude);
  //      for (int i = 0; i < reefs.Count; i++)
  //      {
  //        var reef = reefs[i];
  //        if (reef.Intersects(coordinate))
  //        {
  //          return i;
  //        }
  //      }


}

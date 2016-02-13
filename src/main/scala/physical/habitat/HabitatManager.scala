package physical.habitat

import java.io.File

import grizzled.slf4j.Logging
import io.HabitatFileReader
import locals.{Constants, HabitatType}
import org.geotools.data.simple.SimpleFeatureCollection
import physical.GeoCoordinate
import physical.adaptors.GeometryToGeoCoordinateAdaptor

import scala.collection.mutable.ListBuffer


class HabitatManager(file: File, val buffer: Buffer, habitatTypes: Array[String]) extends Logging {

  private val habitatReader = new HabitatFileReader()
  //val habitats: SimpleFeatureCollection = habitatReader.read(file)
  private val habitatPolygons: List[GeometryAdaptor] = defineAllPolygons(habitatReader.read(file))
  private val reefHabitatPolygons: List[GeometryAdaptor] = habitatPolygons.filter(x => x.habitat == HabitatType.Reef || x.habitat == HabitatType.Other)
  private val landPolygon: List[GeometryAdaptor] = habitatPolygons.filter(x => x.habitat == HabitatType.Land)

  //val filteredHabitats: SimpleFeatureCollection = filterHabitats
  //private val settlementHabitatsHashTable = new collection.mutable.HashMap[Int, HabitatPolygon]
  //val habitatList = ArrayBuffer.empty[HabitatPolygon]
  //private val settlementHabitatsHashTable = defineReefHashTable()
  //private val habitatsHashTable = defineHashTable()

  def isBuffered: Boolean = buffer.isBuffered

  def getReef(index: Int): HabitatPolygon = habitatPolygons(index)

  def defineAllPolygons(habitats: SimpleFeatureCollection): List[GeometryAdaptor] = {

    val polys: ListBuffer[GeometryAdaptor] = ListBuffer.empty[GeometryAdaptor]
    val shapes = habitats.features()
    try {
      while (shapes.hasNext) {
        val shape = shapes.next()
        val geometry = SimpleFeatureAdaptor.getGeometry(shape)

        if (geometry.getNumGeometries > 1) {
          val multipolygon = SimpleFeatureAdaptor.getMultiPolygon(shape)
          for (i <- 0 until geometry.getNumGeometries) {
            polys += new GeometryAdaptor(multipolygon.getGeometryN(i), SimpleFeatureAdaptor.getId(shape), SimpleFeatureAdaptor.getHabitatType(shape))

          }
        } else {
          polys += new GeometryAdaptor(geometry, SimpleFeatureAdaptor.getId(shape), SimpleFeatureAdaptor.getHabitatType(shape))
        }
      }
    } finally {
      shapes.close()
    }
    debug("There are actually this many polygons: " + polys.size)
    polys.toList
  }

  /*  /*
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
    }*/

  //def getReef(index: Int): HabitatPolygon = settlementHabitatsHashTable.get(index).get

  def landStuff(): Unit = {
    debug("The land area has this many points: " + landPolygon.head.coordinates.size)
  }

  def isCoordinateOverReef(coordinate: GeoCoordinate): Int = {
    //debug("Entering isCoordinateOverReef")
    var reefIndex = Constants.NoClosestReefFound
    //val location = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)

    for (i <- reefHabitatPolygons.indices) {
      if (reefHabitatPolygons(i).contains(coordinate)) {
        debug("Coordinate is actually over a reef")
        reefIndex = i
      }
    }
    reefIndex
  }

  def isCoordinateOverBuffer(coordinate: GeoCoordinate): Boolean = {
    val point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)
    buffer.bufferShapes.exists(x => point.within(x))
  }

  def getIndexOfNearestReef(coordinate: GeoCoordinate): Int = {

    var shortestDistance: Double = Double.MaxValue
    var closestReefId: Int = Constants.NoClosestReefFound
    //val point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)

    for (i <- reefHabitatPolygons.indices) {
      val distance = reefHabitatPolygons(i).distance(coordinate)
      debug("Distance between " + coordinate + " and " + reefHabitatPolygons(i).centroid.toString)
      debug("Distance from JTS is " + distance)
      debug("Distance converted is " + getAngulardistance(distance))
      if (distance < buffer.size && distance < shortestDistance) {
        shortestDistance = distance
        closestReefId = i
      }
    }
    closestReefId
  }

  private def getAngulardistance(dist: Double): Double = {
    dist * (Math.PI / 180) * 6378137
  }
  /*
          public int GetIndexOfNearestReef(GeoCoordinate c)
        {
            var geometry = new Geometry();
            double shortestDistance = Double.MaxValue;
            int closestReefId = Constants.NoClosestReefFound;
            foreach (ShapeRange reef in reefs)
            {
                var centroid = new GeoCoordinate(reef.Extent.Center.Y, reef.Extent.Center.Y);
                double distance = geometry.GetDistanceBetweenTwoPoints(c, centroid);
                if (distance < buffer.BufferSize && distance < shortestDistance)
                {
                    shortestDistance = distance;
                    closestReefId = reef.RecordNumber;
                }
            }
            return closestReefId;

        }
   */


  /* private def filterHabitats: SimpleFeatureCollection = {
     //val filterFactory = CQL.toFilter("HABITAT = 'REEF'")
     //val filter  = filterFactory.property
     val filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints)
     val filterList: util.List[Filter] = new util.ArrayList[Filter]()
     habitatTypes foreach (x => filterList.add(filterFactory.equals(filterFactory.property(Constants.ShapeAttribute.Habitat._2), filterFactory.literal(x))))


     //reefFilter.add(ff.equals(ff.property("HABITAT"),ff.literal(HabitatType.Reef)))
     //reefFilter.add(ff.equals(ff.property("HABITAT"),ff.literal(HabitatType.Other)))

     val filter = filterFactory.or(filterList)

     //PropertyName propertyName = ff.property( "testString" );
     //Literal literal = ff.literal( 2 );
     //PropertyIsEqualTo filter = ff.equals( propertyName, literal );
     habitats.subCollection(filter)
   }

   private def defineReefHashTable(): mutable.HashMap[Int, HabitatPolygon] = {

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
         habitatList += habitatPolygon.asInstanceOf[HabitatPolygon]
       }
     } finally {
       shapes.close()
     }
     debug("The reef and other number is "+hash.size)
     hash
   }

   private def defineHashTable(): mutable.HashMap[Int, HabitatPolygon] = {

     val hash = new collection.mutable.HashMap[Int, HabitatPolygon]
     val shapes = habitats.features()
     try {
       while (shapes.hasNext) {
         val shape = shapes.next()
         val geometry = SimpleFeatureAdaptor.getGeometry(shape)
         val id: Int = SimpleFeatureAdaptor.getId(shape)
         val habitatType = SimpleFeatureAdaptor.getHabitatType(shape)
         debug("habitat_type="+habitatType + " and it is of type " + geometry.getGeometryType)
         val habitatPolygon = new GeometryAdaptor(geometry, id, habitatType) with HabitatPolygonToJtsGeometryAdaptor with HabitatPolygon
         hash.put(id, habitatPolygon)
         habitatList += habitatPolygon.asInstanceOf[HabitatPolygon]
       }
     } finally {
       shapes.close()
     }
     debug("The habitat number is "+hash.size)
     hash
   }*/

  def isOcean(coordinate: GeoCoordinate): Boolean = {
    val habitat = getHabitatOfCoordinate(coordinate).habitat
    debug("Habitat is " + habitat)
    habitat != HabitatType.Land && habitat != HabitatType.Beach
  }

  //}

  def getHabitatOfCoordinate(coordinate: GeoCoordinate): HabitatPolygon = {


    for (i <- habitatPolygons.indices) {
      if (habitatPolygons(i).contains(coordinate)) return habitatPolygons(i)
    }
    new GeometryAdaptor(null, Constants.Ocean, HabitatType.Ocean)
  }


  /*
    private def getHabitat(location : Point) : Int = {
      val shapes = habitats.features()
      while (shapes.hasNext) {
        val shape = shapes.next
        val geometry = SimpleFeatureAdaptor.getGeometry(shape)
        //val size = geometry.getNumGeometries
        if (geometry.getNumGeometries > 1) {
          val multipolygon = SimpleFeatureAdaptor.getMultiPolygon(shape)
          for(i<- 0 until geometry.getNumGeometries) {
            val polygon = multipolygon.getGeometryN(i)
            if(location.within(polygon)) return SimpleFeatureAdaptor.getId(shape)
          }
        } else {
          if (location.within(geometry)) return SimpleFeatureAdaptor.getId(shape)
        }
      }
      Constants.Ocean
    }
  */


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

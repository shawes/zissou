package physical.habitat

import java.io.File

import grizzled.slf4j.Logging
import io.GisShapeFile
import locals.HabitatType
import maths.Geometry
import org.geotools.data.simple.SimpleFeatureCollection
import physical.GeoCoordinate

import scala.collection.mutable.ListBuffer


class HabitatManager(file: File, val buffer: Buffer, habitatTypes: Array[String]) extends Logging {

  private val habitatReader = new GisShapeFile()
  //val habitats: SimpleFeatureCollection = habitatReader.read(file)
  private val habitatPolygons: List[GeometryAdaptor] = defineAllPolygons(habitatReader.read(file))
  private val reefHabitatPolygons: List[GeometryAdaptor] =
    habitatPolygons.view.filter(x => x.habitat == HabitatType.Reef || x.habitat == HabitatType.Other).force
  private val bufferedPolygons: List[GeometryAdaptor] = defineAllBufferedPolygons()
  private val landPolygon: List[GeometryAdaptor] = habitatPolygons.filter(x => x.habitat == HabitatType.Land)
  private val geometry = new Geometry
  info("There are this many polygons " + habitatPolygons.size + " of which this many are reefs " + reefHabitatPolygons.size)

  //for (reef <- reefHabitatPolygons) {
  //  debug("Patch num: " + reef.id)
  //}

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
        polys += new GeometryAdaptor(geometry, SimpleFeatureAdaptor.getId(shape), SimpleFeatureAdaptor.getHabitatType(shape))
      }
      polys.toList
    } finally {
      shapes.close()
    }
  }

  def landStuff(): Unit = {
    debug("The land area has this many points: " + landPolygon.head.coordinates.length)
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

  def isCoordinateOverReef(coordinate: GeoCoordinate): Option[Int] = {
    val inside = reefHabitatPolygons.find(x => x.contains(coordinate))
    if (inside.nonEmpty) {
      Some(inside.head.id)
    } else {
      None
    }
  }

  def isCoordinateOverBuffer(coordinate: GeoCoordinate): Option[Int] = {
    val inside = bufferedPolygons.find(x => x.contains(coordinate))
    if (inside.nonEmpty) {
      Some(inside.head.id)
    } else {
      None
    }
  }

  def isCoordinateOverBufferLazy(coordinate: GeoCoordinate, isSettlement: Boolean): Option[Int] = {
    val nearestReefIndex = getIndexOfNearestReef(coordinate)
    val distance = geometry.getDistanceBetweenTwoPoints(coordinate, reefHabitatPolygons(nearestReefIndex).centroid)/1000

    // val distances = reefHabitatPolygons.map(reef => geometry.getDistanceBetweenTwoPoints(coordinate,reef.centroid)
    val threshold = if(isSettlement) buffer.settlement else buffer.olfactory
    debug("Distance is: "+ distance+", threshold is: "+threshold)
    if (distance < threshold) {
      Some(nearestReefIndex)
    } else {
      None
    }
  }


  def getIndexOfNearestReef(coordinate: GeoCoordinate): Int = {

    //val point =

    var shortestDistance: Double = Double.MaxValue
    var closestReefId: Int = reefHabitatPolygons.head.id
    //val point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)

    for (i <- reefHabitatPolygons.indices) {
      //val distance = point.distance(reefHabitatPolygons(i).g)
      //debug("Patch number "+ reefHabitatPolygons(i).id +" is km away "+distance+" with centroid "+reefHabitatPolygons(i).centroid)
      val geomDist = geometry.getDistanceBetweenTwoPoints(coordinate, reefHabitatPolygons(i).centroid)
      //debug("Geometry distance is "+ geomDist)
      if (geomDist < shortestDistance) {
        shortestDistance = geomDist
        closestReefId = i
      }
    }
    debug("The closest reef centroid is: " + reefHabitatPolygons(closestReefId).centroid)
    closestReefId
  }

  private def defineAllBufferedPolygons(): List[GeometryAdaptor] = {
    reefHabitatPolygons.map(reef => new GeometryAdaptor(reef.g.buffer(buffer.settlement / 100), reef.id, reef.habitat))
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

  //  def isOcean(coordinate: GeoCoordinate): Boolean = {
  //    val habitat = getHabitatOfCoordinate(coordinate).habitat
  //    debug("Habitat is " + habitat)
  //    habitat != HabitatType.Land && habitat != HabitatType.Beach
  //  }

  //}

  //  def getHabitatOfCoordinate(coordinate: GeoCoordinate): HabitatPolygon = {
  //    for (i <- habitatPolygons.indices) {
  //      if (habitatPolygons(i).contains(coordinate)) return habitatPolygons(i)
  //    }
  //    new GeometryAdaptor(null, Constants.Ocean, HabitatType.Ocean)
  //  }


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

package physical.habitat

import java.io.File

import grizzled.slf4j.Logging
import io.GisShapeFile
import locals.HabitatType
import maths.Geometry
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.data.collection.ListFeatureCollection
import org.opengis.filter

import physical.GeoCoordinate
import physical.adaptors._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting._
import scala.collection.Searching._


class HabitatManager(file: File, val buffer: Buffer, habitatTypes: Array[String]) extends Logging {

  private val habitatReader = new GisShapeFile()
  private val features = habitatReader.read(file)

  private val reefs = getReefs()
  private val subsetReefs = getSortedCentroids()
  private val centroids = subsetReefs._1
  private val indexes = subsetReefs._2
  //private val fastCollection = getFastCollection()

  //
  // private val reefHabitatPolygons: List[GeometryAdaptor] =
  //   habitatPolygons.view.filter(x => x.habitat == HabitatType.Reef || x.habitat == HabitatType.Other).force
  // private val bufferedPolygons: List[GeometryAdaptor] = defineAllBufferedPolygons()
  // private val landPolygon: List[GeometryAdaptor] = habitatPolygons.filter(x => x.habitat == HabitatType.Land)
  private val geometry = new Geometry
  // info("There are this many polygons " + habitatPolygons.size + " of which this many are reefs " + reefHabitatPolygons.size)

  //for (reef <- reefHabitatPolygons) {
  //  debug("Patch num: " + reef.id)
  //}

  //val filteredHabitats: SimpleFeatureCollection = filterHabitats
  //private val settlementHabitatsHashTable = new collection.mutable.HashMap[Int, HabitatPolygon]
  //val habitatList = ArrayBuffer.empty[HabitatPolygon]
  //private val settlementHabitatsHashTable = defineReefHashTable()
  //private val habitatsHashTable = defineHashTable()


  def isBuffered: Boolean = buffer.isBuffered

  // def getReef(index: Int): HabitatPolygon = habitatPolygons(index)
  //
  def getReefs(): Array[GeometryAdaptor] = {
    val polys:ArrayBuffer[GeometryAdaptor] = ArrayBuffer.empty[GeometryAdaptor]
    val shapes = features.features()
    try {
      while (shapes.hasNext) {
        val shape = shapes.next()
        val geometry = SimpleFeatureAdaptor.getGeometry(shape)
        polys += new GeometryAdaptor(geometry, SimpleFeatureAdaptor.getId(shape), SimpleFeatureAdaptor.getHabitatType(shape))
      }
      polys.toArray
      //quickSort(result)
      //result
    } finally {
      shapes.close()
    }
  }

  private def getSortedCentroids() : (Array[GeoCoordinate],Array[Int]) = {
    val centroidsWithIndexes = reefs.zipWithIndex.map {
      case (reef, count) => (reef.centroid, count)
    }
    quickSort(centroidsWithIndexes)
    centroidsWithIndexes.unzip
  }

  private def getFastCollection() : Array[ArrayBuffer[GeometryAdaptor]] = {
    val latitudeCollection = Array.ofDim[ArrayBuffer[GeometryAdaptor]](180)
    reefs.foreach(reef => {
      val index = reef.centroid.latitude.toInt + 90
      latitudeCollection(index) += reef
    })
    latitudeCollection.foreach(latitude => quickSort(latitude.toArray))
    latitudeCollection
  }

  def getClosestHabitat4(coordinate : GeoCoordinate) : (Int,Int,Double) = {

    val result = centroids.search(coordinate)
    val reef = reefs(indexes(result.insertionPoint))
    var settle = 0
    var sense = 0
    var angle = 0.0

    if(reef.contains(coordinate) || reef.isWithinDistance(coordinate, buffer.settlement)) {
      settle = reef.id
    } else if (reef.isWithinDistance(coordinate, buffer.olfactory)) {
      angle = reef.direction(coordinate)
      sense = reef.id
    }
    //result
    (settle,sense,angle)

  }


    //TODO: Uses brute-force algorithm, need to change to divide and conquer
    // def getClosestHabitat(coordinate: GeoCoordinate): SimpleFeature = {
    //   val location: Point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)
    //   var shortestDistance: Double = Double.MaxValue
    //   var closestReef: SimpleFeature = null
    //   val shapes = filteredHabitats.features()
    //   while (shapes.hasNext) {
    //     val shape = shapes.next()
    //     val geometry = SimpleFeatureAdaptor.getGeometry(shape)
    //     val distance = geometry.distance(location)
    //     if (distance < shortestDistance) {
    //       shortestDistance = distance
    //       closestReef = shape
    //     }
    //   }
    //   closestReef
    // }

    def getClosestHabitat2(coordinate: GeoCoordinate) : (Int, Int, Double) = {
      //val geometry = maths.Geometry()
      val point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)
      var smell = (Double.MaxValue,-1)
      var settle = (Double.MaxValue,-1)
      var inside = -1
      var angle = -1.0
      val iterator = features.features()
      try {
        while(iterator.hasNext) {
          val simpleFeature = iterator.next()
          val reef = SimpleFeatureAdaptor.getGeometry(simpleFeature)
          val id = SimpleFeatureAdaptor.getId(simpleFeature)
          if(reef.contains(point)) {
            inside = id
            return (id, -1, -1)
          } else if(isBuffered) {
            val distance = geometry.getDistanceBetweenTwoPoints(coordinate, GeometryToGeoCoordinateAdaptor.toGeoCoordinate(reef.getCentroid))/1000
            if(distance < buffer.settlement && distance < settle._1) {
              settle = (distance, id)
            }
            if(distance < buffer.olfactory && distance < smell._1) {
              smell = (distance, id)
              angle = geometry.getAngleBetweenTwoPoints(coordinate, GeometryToGeoCoordinateAdaptor.toGeoCoordinate(reef.getCentroid))
            }
          }
        }
        (settle._2, smell._2, angle)
      } finally {
        iterator.close()
      }
    }

    def getClosestHabitat3(coordinate: GeoCoordinate) : (Int, Int, Double) = {
      //val geometry = maths.Geometry()
      //val point = GeometryToGeoCoordinateAdaptor.toPoint(coordinate)
      var smell = 0
      var settle = 0
      var inside = -1
      var angle = -1.0
      val iterator = reefs.toIterator

        while(iterator.hasNext) {
          val reef = iterator.next()
          //val reef = reefPolygon.getGeometry()
          val id = reef.id
          if(reef.contains(coordinate)) {
            inside = id
            return (id, -1, -1)
          } else if(isBuffered) {
            val canSettle = reef.isWithinDistance(coordinate, buffer.settlement)
            if(canSettle) {
              settle = id
            }
            val canSmell = reef.isWithinDistance(coordinate, buffer.olfactory)
            if(canSmell) {
              smell = id
              angle = geometry.getAngleBetweenTwoPoints(coordinate, reef.centroid)
            }
          }
        }
        (settle, smell, angle)
    }

}

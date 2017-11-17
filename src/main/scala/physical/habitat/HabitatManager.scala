package physical.habitat

import java.io.File

import grizzled.slf4j.Logging
import io.GisShapeFile
import locals.HabitatType
import locals.Constants._
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

  private val geometry = new Geometry

  def isBuffered: Boolean = buffer.isBuffered

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

  def getClosestHabitat(coordinate : GeoCoordinate) : (Int,Int,Double) = {

    val result = centroids.search(coordinate)
    val reef = reefs(indexes(result.insertionPoint))
    var settle : Int = LightWeightException.NoReefToSettle
    var sense : Int = LightWeightException.NoReefSensed
    var angle : Double = LightWeightException.NoSwimmingAngle

    val distance = geometry.getDistanceBetweenTwoPoints(coordinate, reef.centroid)/1000
    val distance2 = reef.distance(coordinate) * scala.math.Pi/180 * EarthsRadius * 111.32

    if(reef.isWithinBuffer(coordinate, buffer.settlement)) {
      info(s"between $coordinate and $reef.centroid is distance1 $distance and distance2 $distance2")
      settle = reef.id
    } else if (reef.isWithinBuffer(coordinate, buffer.olfactory)) {
      angle = reef.direction(coordinate)
      sense = reef.id
    }
    (settle,sense,angle)
  }
}

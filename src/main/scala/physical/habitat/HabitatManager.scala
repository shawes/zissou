package physical.habitat

import java.io.File

import grizzled.slf4j.Logging
import io.GisShapeFile
import locals.HabitatType
import locals.Constants.LightWeightException._
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

class HabitatManager(
    file: File,
    val buffer: Buffer,
    habitatTypes: Array[String]
) extends Logging {

  private val habitatReader = new GisShapeFile()
  private val features = habitatReader.read(file)

  private val reefs = getReefs()
  private val subsetReefs = getSortedCentroids()
  private val centroids = subsetReefs._1
  private val indexes = subsetReefs._2

  private val geometry = new Geometry

  def isBuffered: Boolean = buffer.settlement > 0 || buffer.olfactory > 0

  def getReefs(): Array[GeometryAdaptor] = {
    val polygons: ArrayBuffer[GeometryAdaptor] =
      ArrayBuffer.empty[GeometryAdaptor]
    val shapes = features.features()
    try {
      while (shapes.hasNext) {
        val shape = shapes.next()
        val geometry = SimpleFeatureAdaptor.getGeometry(shape)
        polygons += new GeometryAdaptor(
          geometry,
          SimpleFeatureAdaptor.getId(shape),
          SimpleFeatureAdaptor.getHabitatType(shape)
        )
      }
      polygons.toArray
    } finally {
      shapes.close()
    }
  }

  private def getSortedCentroids(): (Array[GeoCoordinate], Array[Int]) = {
    val centroidsWithIndexes = reefs.zipWithIndex.map {
      case (reef, count) => (reef.centroid, count)
    }
    quickSort(centroidsWithIndexes)
    centroidsWithIndexes.unzip
  }

  def getClosestHabitat(coordinate: GeoCoordinate): (Int, Int, Double) = {

    val result = centroids.search(coordinate)
    val reef = reefs(indexes(result.insertionPoint))
    var settle: Int = NoReefToSettleException
    var sense: Int = NoReefSensedException
    var angle: Double = NoSwimmingAngleException

    if (reef.isWithinBuffer(coordinate, buffer.settlement)) {
      settle = reef.id
    } else if (reef.isWithinBuffer(coordinate, buffer.olfactory)) {
      angle = reef.direction(coordinate)
      sense = reef.id
    }
    (settle, sense, angle)
  }
}

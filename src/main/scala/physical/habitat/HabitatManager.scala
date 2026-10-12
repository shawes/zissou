package physical.habitat

import grizzled.slf4j.Logging
import io.config._
import io.GisShapeFile
import locals.Constants.LightWeightException._
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.strtree.STRtree
import physical.GeoCoordinate
import scala.collection.mutable.ArrayBuffer

/**
  * Works out which habitat polygon a larva can settle on, or sense, from where it is.
  *
  * The polygons sit in a spatial index, so a search only measures the few that lie
  * near the larva, and the answer is the nearest one in two dimensions.
  */
class HabitatManager(config: HabitatConfig) extends Logging {

  private val buffer = config.buffer

  private val reefs: Array[GeometryAdaptor] = readReefs(new GisShapeFile().read(config.shapeFilePath))

  // A polygon further away than this can neither be settled on nor sensed
  private val reach: Double = math.max(buffer.settlement, buffer.olfactory)

  private val index: STRtree = {
    val tree = new STRtree()
    reefs.zipWithIndex.foreach { case (reef, position) =>
      if (reef.g != null && !reef.g.isEmpty) tree.insert(reef.g.getEnvelopeInternal, Int.box(position))
    }
    // built here and never changed, so the model's threads can all search it at once
    tree.build()
    tree
  }

  def isBuffered: Boolean = buffer.settlement > 0 || buffer.olfactory > 0

  def getReefs(): Array[GeometryAdaptor] = reefs

  private def readReefs(features: Seq[org.geotools.api.feature.simple.SimpleFeature]): Array[GeometryAdaptor] = {
    val polygons = ArrayBuffer.empty[GeometryAdaptor]
    for (shape <- features) {
      polygons += new GeometryAdaptor(
        SimpleFeatureAdaptor.getGeometry(shape),
        SimpleFeatureAdaptor.getId(shape),
        SimpleFeatureAdaptor.getHabitatType(shape)
      )
    }
    polygons.toArray
  }

  /**
    * What a larva at this position can do about the habitat nearest to it, as
    * (the patch it can settle on, the patch it can sense, the direction to swim to reach it),
    * with the "no reef" codes where there is none. Within the settlement distance of the
    * nearest patch it settles; failing that, within the olfactory distance it senses it.
    */
  def getClosestHabitat(coordinate: GeoCoordinate): (Int, Int, Double) =
    nearest(coordinate) match {
      case Some((reef, kilometres)) if kilometres <= buffer.settlement =>
        (reef.id, NoReefSensedException, NoSwimmingAngleException)
      case Some((reef, kilometres)) if kilometres <= buffer.olfactory =>
        (NoReefToSettleException, reef.id, reef.direction(coordinate))
      case _ =>
        (NoReefToSettleException, NoReefSensedException, NoSwimmingAngleException)
    }

  /** The nearest polygon and its distance in km, among those that could be within reach. */
  private def nearest(coordinate: GeoCoordinate): Option[(GeometryAdaptor, Double)] = {
    var best = -1
    var bestKilometres = Double.PositiveInfinity
    val candidates = index.query(searchArea(coordinate)).iterator()
    while (candidates.hasNext) {
      val position = candidates.next().asInstanceOf[Integer].intValue
      val kilometres = reefs(position).distance(coordinate)
      // a tie goes to the polygon that comes first in the shapefile, so the answer never depends on the search order
      if (kilometres < bestKilometres || (kilometres == bestKilometres && position < best)) {
        best = position
        bestKilometres = kilometres
      }
    }
    if (best < 0) None else Some((reefs(best), bestKilometres))
  }

  // A box around the position that holds every point within `reach` km of it, with room to
  // spare: a degree of latitude is never under 110.5 km and a degree of longitude never over
  // 111.7 km times the cosine of the latitude, so dividing by 110 over-covers on both counts,
  // and the extra 5% allows for a shortest path bowing towards the pole.
  private def searchArea(coordinate: GeoCoordinate): Envelope = {
    val kilometres = reach * 1.05
    val degreesOfLatitude = kilometres / 110.0
    // the box reaches this far from the equator, where a degree of longitude is shortest
    val furthestLatitude = math.min(89.0, math.abs(coordinate.latitude) + degreesOfLatitude)
    val degreesOfLongitude = kilometres / (110.0 * math.cos(math.toRadians(furthestLatitude)))
    new Envelope(
      coordinate.longitude - degreesOfLongitude,
      coordinate.longitude + degreesOfLongitude,
      coordinate.latitude - degreesOfLatitude,
      coordinate.latitude + degreesOfLatitude
    )
  }
}

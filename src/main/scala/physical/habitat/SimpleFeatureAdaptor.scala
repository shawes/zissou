package physical.habitat

import com.vividsolutions.jts.geom.{Geometry, MultiPolygon}
import locals.HabitatType.HabitatType
import locals.{Constants, HabitatType}
import org.opengis.feature.simple.SimpleFeature

object SimpleFeatureAdaptor {
  def getGeometry(sf: SimpleFeature) : Geometry = sf.getAttribute(Constants.ShapeAttribute.Geometry._1).asInstanceOf[Geometry]

  def getMultiPolygon(sf: SimpleFeature): MultiPolygon = sf.getAttribute(Constants.ShapeAttribute.Geometry._1).asInstanceOf[MultiPolygon]

  def getId(sf: SimpleFeature): Int = sf.getAttribute(Constants.ShapeAttribute.Patch._2).asInstanceOf[Int]

  def getHabitatType(sf: SimpleFeature): HabitatType = try {
    HabitatType.withName(sf.getAttribute(Constants.ShapeAttribute.Habitat._2).toString)
  } catch {
    case ex: NoSuchElementException => HabitatType.Ocean
  }

}

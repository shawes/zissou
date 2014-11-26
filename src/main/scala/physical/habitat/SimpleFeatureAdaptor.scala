package physical.habitat

import org.opengis.feature.simple.SimpleFeature
import locals.{HabitatType, Constants}
import com.vividsolutions.jts.geom.Geometry
import locals.HabitatType.HabitatType

object SimpleFeatureAdaptor {
  def getGeometry(sf: SimpleFeature) = sf.getAttribute(Constants.ShapeAttribute.Geometry._1).asInstanceOf[Geometry]

  def getId(sf: SimpleFeature) = sf.getAttribute(Constants.ShapeAttribute.Patch._2).asInstanceOf[Int]

  def getHabitatType(sf: SimpleFeature): HabitatType = try {
    HabitatType.withName(sf.getAttribute(Constants.ShapeAttribute.Habitat._2).toString)
  } catch {
    case ex: NoSuchElementException => HabitatType.Ocean
  }

}

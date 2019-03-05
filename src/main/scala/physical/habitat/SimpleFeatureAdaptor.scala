package physical.habitat

import org.locationtech.jts.geom.{Geometry, MultiPolygon}
import locals.{Constants, HabitatType, Reef, Other, RockyIntertidal, Ocean}
import org.opengis.feature.simple.SimpleFeature

object SimpleFeatureAdaptor {
  def getGeometry(sf: SimpleFeature) : Geometry = sf.getAttribute(Constants.ShapeAttribute.Geometry._1).asInstanceOf[Geometry]

  def getMultiPolygon(sf: SimpleFeature): MultiPolygon = sf.getAttribute(Constants.ShapeAttribute.Geometry._1).asInstanceOf[MultiPolygon]

  def getId(sf: SimpleFeature): Int = sf.getAttribute(Constants.ShapeAttribute.Patch._2).asInstanceOf[Int]

  def getHabitatType(sf: SimpleFeature): HabitatType = {
    sf.getAttribute(Constants.ShapeAttribute.Habitat._2).toString match {
      case "Reef" => Reef
      case "Other" => Other
      case "Rocky intertidal" => RockyIntertidal
      case _ => Ocean
    }
  }

}

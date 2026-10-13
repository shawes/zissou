package tools

import java.io.File
import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import org.geotools.data.shapefile.ShapefileDataStore
import org.locationtech.jts.geom.Geometry
import physical.flow.{Flow, FlowController, FlowRegion}
import physical.{GeoCoordinate, TimeStep}

/** Which reef polygons have BRAN2020 flow at their centre: sbt "Test/runMain tools.ScanReefs <reefs.shp>" */
object ScanReefs {
  val Regions = Seq("Solitary" -> (-31.0, -29.4), "PortStephens" -> (-33.2, -32.2), "Sydney" -> (-34.4, -33.3),
    "JervisBay" -> (-35.3, -34.6), "Ulladulla" -> (-36.0, -35.4), "Eden" -> (-37.5, -36.5))

  def main(args: Array[String]): Unit = {
    val start = new DateTime(2019, 11, 1, 0, 0, DateTimeZone.UTC)
    val flow = new Flow("https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily", start to start.plusDays(5),
      new TimeStep(2, TimeStepType.Hour), true, FlowRegion(-39, -27, 148, 156), 15)
    val iterator = new FlowFileIterator(flow)
    val controller = new FlowController(iterator, flow)
    val time = new LocalDateTime(2019, 11, 1, 12, 0)
    controller.advanceTo(time)

    val store = new ShapefileDataStore(new File(args(0)).toURI.toURL)
    val features = store.getFeatureSource.getFeatures.features()
    case class Reef(id: Int, lat: Double, lon: Double, area: Double, withFlow: Boolean, speed: Double)
    val reefs = scala.collection.mutable.ArrayBuffer.empty[Reef]
    while (features.hasNext) {
      val f = features.next()
      val c = f.getDefaultGeometry.asInstanceOf[Geometry].getCentroid
      val p = new GeoCoordinate(c.getY, c.getX, 5.0)
      val v = scala.util.Try(controller.getVelocityOfCoordinate(p, time)).toOption.flatten
      reefs += Reef(f.getAttribute("PATCH_NUM").asInstanceOf[Number].intValue, c.getY, c.getX,
        f.getDefaultGeometry.asInstanceOf[Geometry].getArea * 12321.0, v.isDefined, v.map(x => math.hypot(x.u, x.v)).getOrElse(0.0))
    }
    features.close(); store.dispose(); iterator.shutdown()
    println(s"SCAN ${reefs.size} reefs, ${reefs.count(_.withFlow)} with BRAN2020 flow at their centre")
    for ((name, (south, north)) <- Regions) {
      val here = reefs.filter(r => r.lat >= south && r.lat <= north)
      println(s"REGION $name: ${here.size} reefs, ${here.count(_.withFlow)} with flow")
      here.filter(_.withFlow).sortBy(-_.area).take(4).foreach(r => println(f"  SITE $name id=${r.id} lat=${r.lat}%.4f lon=${r.lon}%.4f area=${r.area}%.2f km2 speed=${r.speed}%.3f m/s"))
    }
  }
}

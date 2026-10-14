package tools
import java.io.File
import org.geotools.data.shapefile.ShapefileDataStore
import org.locationtech.jts.geom.Geometry
object ReefExtent {
  def main(args: Array[String]): Unit = {
    val store = new ShapefileDataStore(new File(args(0)).toURI.toURL)
    val it = store.getFeatureSource.getFeatures.features()
    var n = 0; var minLat = 90.0; var maxLat = -90.0; val bands = scala.collection.mutable.TreeMap[Double, Int]()
    while (it.hasNext) {
      val c = it.next().getDefaultGeometry.asInstanceOf[Geometry].getCentroid
      n += 1; minLat = math.min(minLat, c.getY); maxLat = math.max(maxLat, c.getY)
      val band = math.floor(c.getY * 2) / 2
      bands(band) = bands.getOrElse(band, 0) + 1
    }
    println(s"EXTENT reefs $n, latitude ${"%.2f".format(minLat)} to ${"%.2f".format(maxLat)}")
    bands.filter(_._1 <= -36.0).foreach { case (b, k) => println(f"EXTENT   $b%.1f to ${b + 0.5}%.1f: $k reefs") }
    store.dispose()
  }
}

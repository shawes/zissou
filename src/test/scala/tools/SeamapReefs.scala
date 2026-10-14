package tools

import java.io.{File, Serializable}
import java.util
import scala.jdk.CollectionConverters._
import org.geotools.api.data.SimpleFeatureStore
import org.geotools.data.DefaultTransaction
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.shapefile.{ShapefileDataStore, ShapefileDataStoreFactory}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.{Geometry, MultiPolygon}

/**
  * Turns pages of Seamap Australia habitat layers (downloaded as shapefiles, one folder per page) into the one
  * shapefile Zissou reads, with a PATCH_NUM and a HABITAT for each polygon. Several layers can be merged: each is
  * `<pages dir>[:<id attribute>[:<offset>]]`, the number of a polygon being its id attribute (ID, or OBJECTID if it has
  * none) plus the offset, so that the layers' numbers do not clash. The server answers with its axes in the order of
  * EPSG:4326, latitude first, which this swaps back.
  *
  * Run with: sbt "Test/runMain tools.SeamapReefs <output .shp> <pages dir>[:<id attribute>[:<offset>]] ..."
  * e.g. NSW as it is, and Victoria's from 1,000,000: data/seamap/pages data/seamap/vic/pages:OID:1000000
  */
object SeamapReefs {

  case class Reef(id: Int, depth: String, geometry: Geometry)

  def main(args: Array[String]): Unit = {
    val reefs = args.drop(1).flatMap { source =>
      val parts = source.split(":")
      val (dir, attribute, offset) = (parts(0), if (parts.length > 1) parts(1) else "ID", if (parts.length > 2) parts(2).toInt else 0)
      val pages = new File(dir).listFiles().filter(_.isDirectory).sortBy(_.getName)
      val here = pages.flatMap(readPage(_, attribute)).map(r => r.copy(id = r.id + offset)).toSeq
      // paging by a key that was not unique repeated some polygons and missed others once; every polygon must be there once
      val distinct = here.map(_.id).distinct.size
      require(distinct == here.size, s"$dir: ${here.size} polygons but only $distinct different ids: a page overlaps another")
      println(s"READ ${here.size} polygons from ${pages.length} pages of $dir (numbers from ${here.map(_.id).min} to ${here.map(_.id).max})")
      here
    }.toSeq
    val distinct = reefs.map(_.id).distinct.size
    require(distinct == reefs.size, s"the layers' patch numbers clash: ${reefs.size} polygons, $distinct numbers; give one an offset")
    println(s"DISTINCT ids: $distinct")
    write(reefs, new File(args(0)))
    println(s"WROTE ${args(0)}")
  }

  private def readPage(page: File, attribute: String): Seq[Reef] = {
    val shp = page.listFiles().find(_.getName.endsWith(".shp")).get
    val store = new ShapefileDataStore(shp.toURI.toURL)
    try {
      val iterator = store.getFeatureSource.getFeatures.features()
      try {
        val out = scala.collection.mutable.ArrayBuffer.empty[Reef]
        while (iterator.hasNext) {
          val f = iterator.next()
          val id = f.getAttribute(attribute) match {
            case n: Number => n.intValue
            case text: String if text.nonEmpty && text.forall(_.isDigit) => text.toInt
            case _ => f.getAttribute("OBJECTID").asInstanceOf[Number].intValue
          }
          out += Reef(id, String.valueOf(f.getAttribute("Depth")), swapAxes(f.getDefaultGeometry.asInstanceOf[Geometry]))
        }
        out.toSeq
      } finally iterator.close()
    } finally store.dispose()
  }

  // The server answered with the axes in the order of EPSG:4326, latitude then longitude; a shapefile, and
  // Zissou, have longitude then latitude
  private def swapAxes(geometry: Geometry): Geometry = {
    val copy = geometry.copy()
    copy.apply(new org.locationtech.jts.geom.CoordinateSequenceFilter {
      def filter(sequence: org.locationtech.jts.geom.CoordinateSequence, i: Int): Unit = {
        val first = sequence.getOrdinate(i, 0)
        sequence.setOrdinate(i, 0, sequence.getOrdinate(i, 1))
        sequence.setOrdinate(i, 1, first)
      }
      def isDone: Boolean = false
      def isGeometryChanged: Boolean = true
    })
    copy
  }

  private def write(reefs: Seq[Reef], file: File): Unit = {
    file.getParentFile.mkdirs()
    val builder = new SimpleFeatureTypeBuilder()
    builder.setName("reefs")
    builder.setCRS(DefaultGeographicCRS.WGS84)
    builder.add("the_geom", classOf[MultiPolygon])
    builder.add("HABITAT", classOf[String])
    builder.add("PATCH_NUM", classOf[java.lang.Integer])
    val featureType = builder.buildFeatureType()
    val factory = new org.locationtech.jts.geom.GeometryFactory()
    val features = new util.ArrayList[org.geotools.api.feature.simple.SimpleFeature]
    for (r <- reefs) {
      val multi: MultiPolygon = r.geometry match {
        case m: MultiPolygon => m
        case p: org.locationtech.jts.geom.Polygon => factory.createMultiPolygon(Array(p))
        case other => throw new IllegalStateException(s"polygon ${r.id} is a ${other.getGeometryType}")
      }
      val b = new SimpleFeatureBuilder(featureType)
      b.add(multi); b.add("Reef"); b.add(Int.box(r.id))
      features.add(b.buildFeature(null))
    }
    val params = new util.HashMap[String, Serializable]()
    params.put("url", file.toURI.toURL)
    params.put("create spatial index", true)
    val store = new ShapefileDataStoreFactory().createNewDataStore(params)
    store.createSchema(featureType)
    val tx = new DefaultTransaction("create")
    try {
      val source = store.getFeatureSource(store.getTypeNames.head).asInstanceOf[SimpleFeatureStore]
      source.setTransaction(tx)
      source.addFeatures(new ListFeatureCollection(featureType, features))
      tx.commit()
    } finally { tx.close(); store.dispose() }
  }
}

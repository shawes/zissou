package io


class ConfigurationFileReader {


  /*
    def read(filePath: String) : Configuration {
      val src = Source.fromFile(new File(filePath))
      val reader = new XMLEventReader(src)
      readXmlElements(reader)
    }

    private def readXmlElements(xml: XMLEventReader): Configuration = {
      var spawningLocations: ArrayBuffer[SpawningLocation] = new ArrayBuffer[SpawningLocation]()
      //var spawn: Spawn = null
      var spawningLocation = null

      while (xml.hasNext) {
        xml.next() match {
          case EvElemStart(_, "SpawningLocation", attributes, _) =>
            val timestep = attributes("timestep").text
            spawningLocation = new SpawningLocation()
          case EvElemStart(_, "dimensions", attributes, _) =>
            //oceanData.grid.width = attributes("longitude").text.toInt
            //oceanData.grid.height = attributes("latitude").text.toInt
            //oceanData.grid.depth = attributes("depth").text.toInt
          case EvElemStart(_, "latitudeRange", attributes, _) =>
            //oceanData.latitudeRange =
              //new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
          case EvElemStart(_, "longitudeRange", attributes, _) =>
            //oceanData.longitudeRange =
              new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
          case EvElemStart(_, "depthRange", attributes, _) =>
            //oceanData.depth.range =
              //new ContinuousRange(attributes("start").text.toDouble, attributes("end").text.toDouble, true)
          case EvElemStart(_, "cellRange", attributes, _) =>
            //oceanData.grid.cell =
             // new Cell(attributes("width").text.toDouble,
              //  attributes("width").text.toDouble,
               // attributes("depth").text.toDouble)
          case EvElemStart(_, "flow", attributes, _) =>
            polygon = new FlowPolygon()
            polygon.id = attributes("id").text.toInt
          case EvElemStart(_, "depth", attributes, _) =>
            val depth = attributes.value.head.toString().toDouble
            polygon.centroid = new GeoCoordinate(0, 0, depth)
          case EvElemStart(_, "salt", attributes, _) =>
            polygon.salinity = attributes.value.head.toString().toDouble
          case EvElemStart(_, "temp", attributes, _) =>
            polygon.temperature = attributes.value.head.toString().toDouble
          case EvElemStart(_, "velocity", attributes, _) =>
            //polygon.velocity = readVelocityElement(attributes)
          case EvElemStart(_, "locus", attributes, _) =>
            //val locus = readLocusElement(attributes)
            //polygon.centroid.latitude = locus.latitude
            //polygon.centroid.longitude = locus.longitude
           // constructArakawaAGrid(polygon, locus, oceanData.grid.cell.width * 0.5)
          case EvElemEnd(_, "flow") =>
            //polygons += polygon
          case _ => ()
        }
      }
      xml.stop()
      new Configuration()
    }*/
}

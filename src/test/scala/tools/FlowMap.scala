package tools

import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import physical.flow.{Flow, FlowController, FlowRegion}
import physical.{GeoCoordinate, TimeStep}

/**
  * Prints BRAN2020's flow over an area as a map: each cell the speed in cm/s and the way it is going, or LAND.
  * sbt "Test/runMain tools.FlowMap south north west east step depth year-month-day"
  */
object FlowMap {
  def main(args: Array[String]): Unit = {
    val Array(south, north, west, east, step, depth, date) = args
    val Array(y, m, d) = date.split("-").map(_.toInt)
    val start = new DateTime(y, m, d, 0, 0, DateTimeZone.UTC)
    val flow = new Flow("https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily", start to start.plusDays(3),
      new TimeStep(2, TimeStepType.Hour), true, FlowRegion(south.toDouble - 1, north.toDouble + 1, west.toDouble - 1, east.toDouble + 1), 15)
    val iterator = new FlowFileIterator(flow)
    val controller = new FlowController(iterator, flow)
    val time = new LocalDateTime(y, m, d, 12, 0)
    controller.advanceTo(time)
    val arrows = Array("E", "NE", "N", "NW", "W", "SW", "S", "SE")
    val h = step.toDouble
    val lons = (0 to math.round((east.toDouble - west.toDouble) / h).toInt).map(i => west.toDouble + i * h)
    val lats = (0 to math.round((north.toDouble - south.toDouble) / h).toInt).map(i => north.toDouble - i * h)
    println(s"MAP depth $depth m on $date; each cell: speed cm/s and direction towards; rows run north to south")
    println("MAP       " + lons.map(l => f"$l%8.2f").mkString(""))
    for (lat <- lats) {
      val row = lons.map { lon =>
        controller.getVelocityOfCoordinate(new GeoCoordinate(lat, lon, depth.toDouble), time) match {
          case None => "    LAND"
          case Some(v) =>
            val angle = (math.toDegrees(math.atan2(v.v, v.u)) + 360 + 22.5) % 360
            f"${math.hypot(v.u, v.v) * 100}%4.0f${arrows((angle / 45).toInt)}%-3s "
        }
      }
      println(f"MAP $lat%6.2f  " + row.mkString(""))
    }
    iterator.shutdown()
  }
}

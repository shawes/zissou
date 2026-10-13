package tools

import com.github.nscala_time.time.Imports._
import io.FlowFileIterator
import locals.TimeStepType
import physical.flow.{Flow, FlowController, FlowRegion}
import physical.{GeoCoordinate, TimeStep}

/** Says which positions BRAN2020 has flow at: sbt "Test/runMain tools.BranSites name:id:lat:lon ..." */
object BranSites {
  def main(args: Array[String]): Unit = {
    val start = new DateTime(2019, 11, 1, 0, 0, DateTimeZone.UTC)
    val flow = new Flow("https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily", start to start.plusDays(5),
      new TimeStep(2, TimeStepType.Hour), true, FlowRegion(-39, -27, 148, 156), 15)
    val iterator = new FlowFileIterator(flow)
    val controller = new FlowController(iterator, flow)
    val time = new LocalDateTime(2019, 11, 1, 12, 0)
    controller.advanceTo(time)
    for (a <- args) {
      val Array(name, id, lat, lon) = a.split(":")
      val p = new GeoCoordinate(lat.toDouble, lon.toDouble, 5.0)
      val nearest = scala.util.Try(controller.getVelocity(p, time)).toOption.flatten
      val interpolated = scala.util.Try(controller.getVelocityOfCoordinate(p, time)).toOption.flatten
      println(f"SITE $name id=$id ($lat, $lon): nearest=${nearest.map(v => f"${math.hypot(v.u, v.v)}%.2f m/s").getOrElse("LAND")} interpolated=${interpolated.map(v => f"${math.hypot(v.u, v.v)}%.2f m/s").getOrElse("LAND")}")
    }
    iterator.shutdown()
  }
}

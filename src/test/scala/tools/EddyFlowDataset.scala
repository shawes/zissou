package tools

import java.io.File
import locals.Constants

/**
  * A steady eddy in solid-body rotation: the flow goes round a centre at an angular speed omega
  * (u = -omega * north, v = omega * east, with north and east the metres from the centre), so it
  * is the same on every day, and the exact path of a larva is a circle about the centre. The grid
  * covers a degree each way from the centre.
  */
object EddyFlowDataset {

  val CentreLongitude = 153.5
  val CentreLatitude = -28.0

  private val Lons = GriddedFlowDataset.axis(152.5, 154.5, 0.1)
  private val Lats = GriddedFlowDataset.axis(-29.0, -27.0, 0.1)

  /** Metres east and north of the centre of a position, measured the way the integrator moves a larva. */
  def metresFromCentre(longitude: Double, latitude: Double): (Double, Double) =
    (
      (longitude - CentreLongitude) * math.Pi / 180 * Constants.EarthsRadius * math.cos(latitude * math.Pi / 180),
      (latitude - CentreLatitude) * math.Pi / 180 * Constants.EarthsRadius
    )

  def write(root: File, months: Seq[(Int, Int)], omega: Double): Unit =
    GriddedFlowDataset.write(root, months, Lons, Lats) { (variable, longitude, latitude, _) =>
      val (east, north) = metresFromCentre(longitude, latitude)
      variable match {
        case "u" => -omega * north
        case "v" => omega * east
        case _   => 0.0
      }
    }
}

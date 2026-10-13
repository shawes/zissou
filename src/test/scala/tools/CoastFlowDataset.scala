package tools

import java.io.File

/**
  * Monthly flow files with a coast: land, where there is no flow (not a number), west of 153.35 east, and sea east of it,
  * where the flow is the same everywhere, `u` m/s east and `v` m/s north. The grid is 153.0 to 154.0 east and 28.5 to 27.5
  * south at 0.1 degrees, so the first sea node is 153.4 and the land and the sea are divided at 153.35, where the nearest
  * node changes.
  */
object CoastFlowDataset {
  val CoastLongitude = 153.35

  def write(root: File, months: Seq[(Int, Int)], u: Double, v: Double): Unit =
    GriddedFlowDataset.write(root, months, GriddedFlowDataset.axis(153.0, 154.0, 0.1), GriddedFlowDataset.axis(-28.5, -27.5, 0.1)) {
      (variable, lon, _, _) =>
        if (lon < CoastLongitude) Double.NaN
        else variable match { case "u" => u; case "v" => v; case _ => 0.0 }
    }
}

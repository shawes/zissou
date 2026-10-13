package tools

import java.io.File

/**
  * Monthly flow files over a shelf: sea everywhere on the grid (153 to 154 east, 28.5 to 27.5 south), with a sea floor
  * 30 m down, so that there is flow in the levels at 2.5 to 28.2 m and none below, as in an ocean model's data. The flow is
  * `u` m/s east and `v` m/s north, and the temperature, if there is one, falls 0.1 degrees for every metre of depth from
  * 25 degrees at the surface (the files do not have depth in it, so it is the same in the levels there are: 25 degrees).
  */
object ShelfFlowDataset {
  val FloorDepth = 30.0

  def write(root: File, months: Seq[(Int, Int)], u: Double, v: Double, withTemperature: Boolean = false): Unit =
    GriddedFlowDataset.write(
      root, months, GriddedFlowDataset.axis(153.0, 154.0, 0.1), GriddedFlowDataset.axis(-28.5, -27.5, 0.1), None,
      if (withTemperature) Seq("u", "v", "w", "temp") else Seq("u", "v", "w"), Some((_, _) => FloorDepth)
    ) { (variable, _, _, _) =>
      variable match { case "u" => u; case "v" => v; case "temp" => 25.0; case _ => 0.0 }
    }
}

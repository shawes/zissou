package tools

import java.io.File
import java.time.LocalDate

/**
  * Monthly NetCDF flow files in which every field is the same everywhere and equal to a
  * multiple of the day it belongs to, counted from 2002-01-01. Reading a velocity back
  * therefore says exactly which day's field it came from, and a blend of two days is
  * visible as a day number with a fraction.
  *
  *   u = 0.01 * day,  v = -0.005 * day,  w = 0.000001 * day
  *
  * The grid lies inside the area FlowFileIterator reads, with its 15 depth levels, and the
  * daily fields are stamped at noon, like the daily means of an ocean model.
  */
object UniformFlowDataset {

  val Epoch: LocalDate = LocalDate.of(2002, 1, 1)
  val U = 0.01
  val V = -0.005
  val W = 0.000001

  private val Lons = GriddedFlowDataset.axis(153.0, 154.0, 0.1)
  private val Lats = GriddedFlowDataset.axis(-28.5, -27.5, 0.1)

  /** The day number a date has in these files. */
  def dayNumber(date: LocalDate): Long = java.time.temporal.ChronoUnit.DAYS.between(Epoch, date)

  def dayNumber(year: Int, month: Int, day: Int): Long = dayNumber(LocalDate.of(year, month, day))

  /** Writes one file per (year, month) under root/u, root/v and root/w. */
  def write(root: File, months: Seq[(Int, Int)]): Unit =
    GriddedFlowDataset.write(root, months, Lons, Lats) { (variable, _, _, day) =>
      day * (variable match { case "u" => U; case "v" => V; case _ => W })
    }
}

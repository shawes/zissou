package io

import java.io.File
import java.time.YearMonth
import grizzled.slf4j.Logging
import ucar.nc2.dt.grid.GridDataset

/**
  * Where the monthly flow files come from. A month has one file for each of the variables u, v
  * and w, named `<name>_<variable>_<year>_<month>.nc`, each holding a field for every day of
  * the month.
  */
trait FlowSource {

  /** Whether the files for the month are there. */
  def has(month: YearMonth): Boolean

  /** The first month after this one that there are files for, if there is one. */
  def firstAfter(month: YearMonth): Option[YearMonth]

  /** Opens the file of one variable for a month. */
  def open(variable: String, month: YearMonth): GridDataset

  /** Where the files are, for messages. */
  def description: String
}

/** A folder holding a sub-folder for each variable, each with the monthly files of that variable. */
class LocalFlowSource(path: String, variables: List[String]) extends FlowSource with Logging {

  private val NetcdfExtension = ".nc"

  // the file of each month, for each variable
  private val files: Map[String, Map[YearMonth, String]] =
    variables.map(variable => variable -> filesOf(variable)).toMap

  files(variables.head).values.toSeq.sorted.foreach(name => info(s"$name"))

  private def filesOf(variable: String): Map[YearMonth, String] = {
    val folder = new File(path, variable)
    val names = Option(folder.list()).getOrElse(Array.empty[String]).filter(_.endsWith(NetcdfExtension)).sorted
    names.flatMap(name => monthOf(name).map(_ -> name)).toMap
  }

  private def monthOf(filename: String): Option[YearMonth] = filename match {
    case LocalFlowSource.MonthlyFile(year, month) if month.toInt >= 1 && month.toInt <= 12 => Some(YearMonth.of(year.toInt, month.toInt))
    case _ =>
      debug(s"Ignoring $filename, which is not named like a month's flow")
      None
  }

  // the first variable's files say which months there are
  private def months: Seq[YearMonth] = files(variables.head).keys.toSeq

  def has(month: YearMonth): Boolean = files(variables.head).contains(month)

  def firstAfter(month: YearMonth): Option[YearMonth] =
    months.filter(_.isAfter(month)).sorted.headOption

  def open(variable: String, month: YearMonth): GridDataset = {
    val name = files(variable).getOrElse(
      month,
      throw new IllegalArgumentException(s"There is no $variable flow file for $month in $path/$variable")
    )
    GridDataset.open(s"$path/$variable/$name")
  }

  def description: String = s"$path/${variables.head}"
}

object LocalFlowSource {
  // <name>_<variable>_<year>_<month>.nc; a file that is not named like that is not a month's flow
  val MonthlyFile = """.*_(\d{4})_(\d{1,2})\.nc""".r
}

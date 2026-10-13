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

  /** Whether reading from it is slow enough to be worth doing ahead of time. */
  def isRemote: Boolean = false

  /** Runs a read from the source, which a source that can fail for a while may try again. */
  def retrying[T](what: String)(body: => T): T = body
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

/**
  * Monthly files on an OPeNDAP server, such as a THREDDS `dodsC` directory:
  * `<base>/<prefix>_<variable>_<year>_<month>.nc`, with the three variables side by side
  * in the one directory, e.g. https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily
  * for BRAN2020. Nothing is downloaded but the part of each field that is read.
  *
  * A server cannot list its files, so a month is there if the server answers for its file,
  * and the files end at the first month it has none for: a gap in them cannot be told from
  * the end. A request that fails other than by the file not being there is tried again
  * `attempts` times, a little longer after each, before the run fails with it.
  */
class OpenDapFlowSource(
    base: String,
    prefix: String = "ocean",
    variables: List[String] = FlowFileIterator.Variables,
    attempts: Int = 4,
    pauseMillis: Long = 1000,
    timeoutSeconds: Int = OpenDapFlowSource.TimeoutSeconds
) extends FlowSource with Logging {

  private val root = base.stripSuffix("/")
  private val available = new java.util.concurrent.ConcurrentHashMap[YearMonth, java.lang.Boolean]()

  OpenDapFlowSource.setTimeouts(timeoutSeconds)

  def url(variable: String, month: YearMonth): String =
    f"$root/${prefix}_${variable}_${month.getYear}%d_${month.getMonthValue}%02d.nc"

  override def isRemote: Boolean = true

  def has(month: YearMonth): Boolean =
    available.computeIfAbsent(month, m => variables.forall(variable => exists(url(variable, m)))).booleanValue

  def firstAfter(month: YearMonth): Option[YearMonth] =
    Some(month.plusMonths(1)).filter(has)

  def open(variable: String, month: YearMonth): GridDataset = {
    val address = url(variable, month)
    retrying(s"opening $address")(GridDataset.open(address))
  }

  override def retrying[T](what: String)(body: => T): T = {
    var attempt = 1
    while (true) {
      try return body
      catch {
        case e: java.io.IOException if attempt < attempts =>
          warn(s"Failed $what (attempt $attempt of $attempts): ${e.getMessage}")
          Thread.sleep(pauseMillis * attempt)
          attempt += 1
      }
    }
    throw new IllegalStateException("unreachable")
  }

  def description: String = root

  // whether the server has the file: it answers for the file's structure (.dds) if it does
  private def exists(address: String): Boolean = retrying(s"asking for $address") {
    val request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(address + ".dds"))
      .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
      .GET().build()
    val status = OpenDapFlowSource.client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode
    if (status == 200) true
    else if (status == 404) false
    else throw new java.io.IOException(s"$address answered $status")
  }
}

object OpenDapFlowSource {
  val TimeoutSeconds = 120

  private lazy val client: java.net.http.HttpClient = java.net.http.HttpClient.newBuilder()
    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
    .connectTimeout(java.time.Duration.ofSeconds(30))
    .build()

  // the library's own limit on a request, in milliseconds: its default is to wait for ever
  private def setTimeouts(seconds: Int): Unit = {
    ucar.httpservices.HTTPSession.setGlobalConnectionTimeout(30 * 1000)
    ucar.httpservices.HTTPSession.setGlobalSoTimeout(seconds * 1000)
  }

  /** Whether a flow path is the address of a server rather than a folder. */
  def isUrl(path: String): Boolean = path.startsWith("http://") || path.startsWith("https://")
}

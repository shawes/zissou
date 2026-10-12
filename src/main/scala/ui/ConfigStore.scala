package ui

import java.nio.file.{Files, Path, StandardCopyOption}
import com.github.nscala_time.time.Imports.{DateTime, DateTimeZone}
import io.circe.{CursorOp, DecodingFailure, Encoder, ParsingFailure}
import io.config.{Configuration, ConfigurationFile}
import scala.jdk.CollectionConverters.*
import scala.util.Try

case class ConfigInfo(name: String, modified: Long) derives Encoder.AsObject

case class SiteSummary(
    name: String,
    patch: Int,
    latitude: Double,
    longitude: Double,
    larvae: Int,
    releaseStart: String,
    releaseEnd: String,
    interval: Int
) derives Encoder.AsObject

case class ConfigSummary(
    periodStart: String,
    periodEnd: String,
    timeStep: String,
    species: String,
    sites: Seq[SiteSummary]
) derives Encoder.AsObject

case class Validation(
    ok: Boolean,
    error: Option[String],
    summary: Option[ConfigSummary],
    warnings: Seq[String]
) derives Encoder.AsObject

/** The configuration files in one directory, and checks on their contents. */
class ConfigStore(val dir: Path, baseDir: Path) {

  private val NamePattern = "^[A-Za-z0-9][A-Za-z0-9._-]*\\.ya?ml$".r

  // The grid limits hard-coded in FlowFileIterator
  private val LatitudeRange = (-50.0, -10.0)
  private val LongitudeRange = (142.0, 180.0)

  /** Only plain file names are allowed, so a name can never leave the directory. */
  def isValidName(name: String): Boolean = NamePattern.matches(name)

  def list(): Seq[ConfigInfo] =
    if (!Files.isDirectory(dir)) Seq.empty
    else {
      val stream = Files.list(dir)
      try {
        stream.iterator().asScala
          .filter(p => Files.isRegularFile(p) && isValidName(p.getFileName.toString))
          .map(p => ConfigInfo(p.getFileName.toString, Files.getLastModifiedTime(p).toMillis))
          .toSeq
          .sortBy(_.name)
      } finally stream.close()
    }

  def read(name: String): Option[String] =
    if (!isValidName(name)) None
    else {
      val path = dir.resolve(name)
      if (Files.isRegularFile(path)) Some(Files.readString(path)) else None
    }

  def write(name: String, text: String): Unit = {
    require(isValidName(name), s"Not a valid configuration name: $name")
    Files.createDirectories(dir)
    // write to a temporary file first, so a failure never leaves half a config behind
    val temporary = Files.createTempFile(dir, ".config-", ".tmp")
    try {
      Files.writeString(temporary, text)
      Files.move(temporary, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING)
    } finally Files.deleteIfExists(temporary)
  }

  def validate(text: String): Validation =
    new ConfigurationFile().parse(text) match {
      case Left(failure) => Validation(false, Some(describe(failure)), None, Seq.empty)
      case Right(config) =>
        invalidDate(config) match {
          case Some(problem) => Validation(false, Some(problem), None, Seq.empty)
          case None => Validation(true, None, Some(summarise(config)), warningsFor(config))
        }
    }

  private def describe(failure: io.circe.Error): String = failure match {
    case parse: ParsingFailure =>
      // the parser's own message repeats the offending text over several lines
      val first = parse.message.linesIterator.nextOption().getOrElse("").trim
      val position = """line (\d+), column (\d+)""".r.findFirstMatchIn(parse.message)
        .map(m => s" (line ${m.group(1)}, column ${m.group(2)})").getOrElse("")
      s"The file is not valid YAML: $first$position"
    case decode: DecodingFailure =>
      val field = CursorOp.opsToPath(decode.history).stripPrefix(".")
      val subject = if (field.isEmpty) "The configuration" else field
      decode.message match {
        case "Int" | "Long" => s"$subject must be a whole number."
        case "Double" | "Float" => s"$subject must be a number."
        case "String" => s"$subject must be text."
        case "Boolean" => s"$subject must be true or false."
        case m if m.contains("Missing required field") => s"$subject is missing."
        case m => s"$subject: $m"
      }
  }

  private def isDate(text: String): Boolean =
    Try(new DateTime(text, DateTimeZone.UTC)).isSuccess

  private def invalidDate(config: Configuration): Option[String] = {
    val dates =
      Seq("flow.period.start" -> config.flow.period.start, "flow.period.end" -> config.flow.period.end) ++
        config.spawn.spawningLocation.flatMap(site =>
          Seq(
            s"release start of ${site.name}" -> site.releasePeriod.start,
            s"release end of ${site.name}" -> site.releasePeriod.end
          )
        )
    dates.collectFirst { case (where, value) if !isDate(value) => s"'$value' is not a valid date ($where)" }
  }

  private def summarise(config: Configuration): ConfigSummary =
    ConfigSummary(
      config.flow.period.start,
      config.flow.period.end,
      s"${config.flow.timeStep.duration} ${config.flow.timeStep.unit.toLowerCase}",
      config.larva.species,
      config.spawn.spawningLocation.map(s =>
        SiteSummary(
          s.name, s.patchNumber, s.site.latitude, s.site.longitude, s.numberOfLarvae,
          s.releasePeriod.start, s.releasePeriod.end, s.interval
        )
      )
    )

  private def warningsFor(config: Configuration): Seq[String] = {
    val warnings = Seq.newBuilder[String]

    def missing(path: String): Boolean = !Files.exists(baseDir.resolve(path))
    val flow = config.flow.netcdfFilePath
    if (missing(flow)) warnings += s"The flow folder '$flow' does not exist."
    else {
      val absent = Seq("u", "v", "w").filter(v => missing(s"$flow/$v"))
      if (absent.nonEmpty) warnings += s"The flow folder '$flow' has no ${absent.mkString(", ")} sub-folder."
    }
    if (missing(config.habitat.shapeFilePath)) {
      warnings += s"The habitat shapefile '${config.habitat.shapeFilePath}' does not exist."
    }

    val start = new DateTime(config.flow.period.start, DateTimeZone.UTC)
    val sites = config.spawn.spawningLocation
    val seasonAtStart = sites.exists { s =>
      !start.isBefore(new DateTime(s.releasePeriod.start, DateTimeZone.UTC)) &&
      start.isBefore(new DateTime(s.releasePeriod.end, DateTimeZone.UTC))
    }
    if (sites.nonEmpty && !seasonAtStart) {
      warnings += "The flow period does not start inside any release period, so the simulation will end immediately. " +
        "Start the flow period on the first release day."
    }

    sites.foreach { s =>
      val (south, north) = LatitudeRange
      val (west, east) = LongitudeRange
      if (s.site.latitude < south || s.site.latitude > north || s.site.longitude < west || s.site.longitude > east) {
        warnings += s"${s.name} lies outside the supported flow grid (latitude $south to $north, longitude $west to $east)."
      }
    }
    // release days are counted with Period.getDays, which only works out for 1 and 7
    sites.filterNot(s => s.interval == 1 || s.interval == 7).foreach { s =>
      warnings += s"${s.name} releases every ${s.interval} days: release days are currently miscounted " +
        "for any interval other than 1 or 7."
    }
    if (config.larva.pelagicLarvalDuration.pldType.equalsIgnoreCase("fixed") &&
      config.larva.pelagicLarvalDuration.pldType != "fixed") {
      warnings += "pldType is case-sensitive: only 'fixed' (lower case) gives a fixed duration, " +
        s"'${config.larva.pelagicLarvalDuration.pldType}' samples one from a normal distribution."
    }
    warnings.result()
  }
}

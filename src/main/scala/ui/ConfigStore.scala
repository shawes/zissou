package ui

import java.nio.file.{Files, Path, StandardCopyOption}
import com.github.nscala_time.time.Imports.{DateTime, DateTimeZone}
import io.circe.{CursorOp, DecodingFailure, Encoder, ParsingFailure}
import io.config.{Configuration, ConfigurationFile, LarvaChecks}
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
        invalidDate(config).orElse(invalidInterval(config)).orElse(invalidDiffusion(config)).orElse(LarvaChecks.problems(config.larva).headOption).orElse(invalidFlowRegion(config)).orElse(invalidFlowAddress(config)) match {
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

  // the release schedule divides by the interval, so zero would crash the run
  private def invalidInterval(config: Configuration): Option[String] =
    config.spawn.spawningLocation.collectFirst {
      case site if site.interval < 1 => s"The release interval of ${site.name} must be at least 1 day."
    }

  // the random walk takes the square root of the diffusion coefficient
  private def invalidDiffusion(config: Configuration): Option[String] = {
    val t = config.turbulence
    Seq("horizontalDiffusionCoefficient" -> t.horizontalDiffusionCoefficient, "verticalDiffusionCoefficient" -> t.verticalDiffusionCoefficient)
      .collectFirst { case (name, value) if value < 0 || value.isNaN => s"turbulence.$name must be zero or more." }
  }

  // an address is a directory on an OPeNDAP server: the files are <address>/<prefix>_<variable>_<year>_<month>.nc
  private def invalidFlowAddress(config: Configuration): Option[String] = {
    val paths = ("flow.netcdfFilePath" -> config.flow.netcdfFilePath) +:
      config.flow.nested.getOrElse(List.empty).zipWithIndex.map { case (n, i) => s"flow.nested[$i].netcdfFilePath" -> n.netcdfFilePath }
    paths.flatMap { case (name, path) =>
      if (!io.OpenDapFlowSource.isUrl(path)) None
      else if (scala.util.Try(java.net.URI.create(path)).toOption.forall(uri => uri.getHost == null))
        Some(s"$name '$path' is not a valid address.")
      else if (path.contains("?") || path.endsWith(".nc"))
        Some(s"$name must be the directory of the monthly files on the server, without a file name or query: '$path'.")
      else None
    }.headOption
  }

  // the part of the flow grid that is read, and how many depth levels of it
  private def invalidFlowRegion(config: Configuration): Option[String] = {
    val flow = config.flow
    flow.bounds.collectFirst {
      case b if !(b.south < b.north) => s"flow.bounds.south (${b.south}) must be less than flow.bounds.north (${b.north})."
      case b if !(b.west < b.east) => s"flow.bounds.west (${b.west}) must be less than flow.bounds.east (${b.east})."
    }.orElse(flow.depthLevels.collectFirst { case n if n < 1 => s"flow.depthLevels must be at least 1, not $n." })
      .orElse(flow.nested.getOrElse(List.empty).zipWithIndex.collectFirst {
        case (n, i) if !(n.bounds.south < n.bounds.north) =>
          s"flow.nested[$i].bounds.south (${n.bounds.south}) must be less than its north (${n.bounds.north})."
        case (n, i) if !(n.bounds.west < n.bounds.east) =>
          s"flow.nested[$i].bounds.west (${n.bounds.west}) must be less than its east (${n.bounds.east})."
        case (n, i) if n.depthLevels.exists(_ < 1) => s"flow.nested[$i].depthLevels must be at least 1."
      })
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
    // an address is not checked here: that takes the network, and the run says if the server has no data
    if (io.OpenDapFlowSource.isUrl(flow)) ()
    else if (missing(flow)) warnings += s"The flow folder '$flow' does not exist."
    else {
      val absent = Seq("u", "v", "w").filter(v => missing(s"$flow/$v"))
      if (absent.nonEmpty) warnings += s"The flow folder '$flow' has no ${absent.mkString(", ")} sub-folder."
    }
    config.flow.nested.getOrElse(List.empty).zipWithIndex.foreach { case (n, i) =>
      if (!io.OpenDapFlowSource.isUrl(n.netcdfFilePath) && missing(n.netcdfFilePath))
        warnings += s"The nested flow folder '${n.netcdfFilePath}' (flow.nested[$i]) does not exist."
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

    val region = config.flow.bounds
      .map(b => physical.flow.FlowRegion(b.south, b.north, b.west, b.east))
      .getOrElse(physical.flow.FlowRegion.Default)
    sites.foreach { s =>
      if (!region.contains(s.site.latitude, s.site.longitude)) {
        warnings += s"${s.name} lies outside the flow region that is read (latitude ${region.south} to ${region.north}, " +
          s"longitude ${region.west} to ${region.east})."
      }
    }
    if (config.larva.pelagicLarvalDuration.pldType.equalsIgnoreCase("fixed") &&
      config.larva.pelagicLarvalDuration.pldType != "fixed") {
      warnings += "pldType is case-sensitive: only 'fixed' (lower case) gives a fixed duration, " +
        s"'${config.larva.pelagicLarvalDuration.pldType}' samples one from a normal distribution."
    }
    warnings.result()
  }
}

package ui

import java.nio.file.{Files, Path, StandardCopyOption}
import com.github.nscala_time.time.Imports.{DateTime, DateTimeZone}
import io.circe.{CursorOp, DecodingFailure, Encoder, ParsingFailure}
import io.config.{BoundsConfig, Configuration, ConfigurationFile, LarvaChecks}
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

/**
  * One thing wrong with a configuration. `path` is where, as the dotted field names the messages
  * use (`spawn.spawningLocation[0].interval`), empty when it is the file as a whole; `line` and
  * `column` (from 1) are given when the YAML itself cannot be read.
  */
case class Problem(message: String, path: String = "", line: Option[Int] = None, column: Option[Int] = None) derives Encoder.AsObject

case class Validation(
    ok: Boolean,
    error: Option[String],
    summary: Option[ConfigSummary],
    warnings: Seq[String],
    problems: Seq[Problem] = Seq.empty
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
      case Left(failure) => failed(Seq(describe(failure)))
      case Right(config) =>
        // every check runs, so that fixing one problem does not just reveal the next
        val problems = invalidDate(config) ++ invalidInterval(config) ++ invalidDiffusion(config) ++
          LarvaChecks.problems(config.larva).map(larvaProblem) ++ invalidFlowRegion(config) ++ invalidFlowAddress(config)
        if (problems.nonEmpty) failed(problems)
        else Validation(true, None, Some(summarise(config)), warningsFor(config))
    }

  private def failed(problems: Seq[Problem]): Validation =
    Validation(false, problems.headOption.map(_.message), None, Seq.empty, problems)

  // LarvaChecks messages begin with the field they are about
  private def larvaProblem(message: String): Problem =
    Problem(message, """^larva(\.\w+)*""".r.findFirstIn(message).getOrElse(""))

  private def describe(failure: io.circe.Error): Problem = failure match {
    case parse: ParsingFailure =>
      // the parser's own message repeats the offending text over several lines
      val first = parse.message.linesIterator.nextOption().getOrElse("").trim
      val at = """line (\d+), column (\d+)""".r.findFirstMatchIn(parse.message)
      val position = at.map(m => s" (line ${m.group(1)}, column ${m.group(2)})").getOrElse("")
      Problem(s"The file is not valid YAML: $first$position", "", at.map(_.group(1).toInt), at.map(_.group(2).toInt))
    case decode: DecodingFailure =>
      val field = CursorOp.opsToPath(decode.history).stripPrefix(".")
      val subject = if (field.isEmpty) "The configuration" else field
      val message = decode.message match {
        case "Int" | "Long" => s"$subject must be a whole number."
        case "Double" | "Float" => s"$subject must be a number."
        case "String" => s"$subject must be text."
        case "Boolean" => s"$subject must be true or false."
        case m if m.contains("Missing required field") => s"$subject is missing."
        case m => s"$subject: $m"
      }
      Problem(message, field)
  }

  private def isDate(text: String): Boolean =
    Try(new DateTime(text, DateTimeZone.UTC)).isSuccess

  private def invalidDate(config: Configuration): Seq[Problem] = {
    val dates =
      Seq(("flow.period.start", "flow.period.start", config.flow.period.start), ("flow.period.end", "flow.period.end", config.flow.period.end)) ++
        config.spawn.spawningLocation.zipWithIndex.flatMap { case (site, i) =>
          val at = s"spawn.spawningLocation[$i].releasePeriod"
          Seq(
            (s"release start of ${site.name}", s"$at.start", site.releasePeriod.start),
            (s"release end of ${site.name}", s"$at.end", site.releasePeriod.end)
          )
        }
    dates.collect { case (where, path, value) if !isDate(value) => Problem(s"'$value' is not a valid date ($where)", path) }
  }

  // the release schedule divides by the interval, so zero would crash the run
  private def invalidInterval(config: Configuration): Seq[Problem] =
    config.spawn.spawningLocation.zipWithIndex.collect {
      case (site, i) if site.interval < 1 =>
        Problem(s"The release interval of ${site.name} must be at least 1 day.", s"spawn.spawningLocation[$i].interval")
    }

  // the random walk takes the square root of the diffusion coefficient
  private def invalidDiffusion(config: Configuration): Seq[Problem] = {
    val t = config.turbulence
    Seq("horizontalDiffusionCoefficient" -> t.horizontalDiffusionCoefficient, "verticalDiffusionCoefficient" -> t.verticalDiffusionCoefficient)
      .collect { case (name, value) if value < 0 || value.isNaN => Problem(s"turbulence.$name must be zero or more.", s"turbulence.$name") }
  }

  // an address is a directory on an OPeNDAP server: the files are <address>/<prefix>_<variable>_<year>_<month>.nc
  private def invalidFlowAddress(config: Configuration): Seq[Problem] = {
    val paths = ("flow.netcdfFilePath" -> config.flow.netcdfFilePath) +:
      config.flow.nested.getOrElse(List.empty).zipWithIndex.map { case (n, i) => s"flow.nested[$i].netcdfFilePath" -> n.netcdfFilePath }
    paths.flatMap { case (name, path) =>
      if (!io.OpenDapFlowSource.isUrl(path)) None
      else if (scala.util.Try(java.net.URI.create(path)).toOption.forall(uri => uri.getHost == null))
        Some(Problem(s"$name '$path' is not a valid address.", name))
      else if (path.contains("?") || path.endsWith(".nc"))
        Some(Problem(s"$name must be the directory of the monthly files on the server, without a file name or query: '$path'.", name))
      else None
    }
  }

  // the part of the flow grid that is read, and how many depth levels of it
  private def invalidFlowRegion(config: Configuration): Seq[Problem] = {
    val flow = config.flow
    def check(path: String, label: String, bounds: BoundsConfig, depthLevels: Option[Int]): Seq[Problem] =
      Seq(
        Option.when(!(bounds.south < bounds.north))(
          Problem(s"$label.south (${bounds.south}) must be less than $label.north (${bounds.north}).", s"$path.bounds.south")),
        Option.when(!(bounds.west < bounds.east))(
          Problem(s"$label.west (${bounds.west}) must be less than $label.east (${bounds.east}).", s"$path.bounds.west")),
        depthLevels.collect { case n if n < 1 => Problem(s"$path.depthLevels must be at least 1, not $n.", s"$path.depthLevels") }
      ).flatten
    flow.bounds.toSeq.flatMap(b => check("flow", "flow.bounds", b, None)) ++
      flow.depthLevels.collect { case n if n < 1 => Problem(s"flow.depthLevels must be at least 1, not $n.", "flow.depthLevels") } ++
      flow.nested.getOrElse(List.empty).zipWithIndex.flatMap { case (n, i) =>
        check(s"flow.nested[$i]", s"flow.nested[$i].bounds", n.bounds, n.depthLevels)
      }
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
      // the temperature is read, from a folder of its own, only if mortality depends on it
      val variables = Seq("u", "v", "w") ++ (if (config.larva.temperatureMortality.isDefined) Seq("temp") else Seq.empty)
      val absent = variables.filter(v => missing(s"$flow/$v"))
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

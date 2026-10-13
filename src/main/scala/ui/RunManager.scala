package ui

import java.nio.file.{Files, Path}
import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*
import io.config.ConfigurationFile
import scala.jdk.CollectionConverters.*
import scala.util.Try

case class RunInfo(
    id: String,
    config: String,
    status: String, // running, succeeded, failed, cancelled or interrupted
    startedAt: Long,
    endedAt: Option[Long],
    exitCode: Option[Int],
    message: Option[String],
    periodStart: String,
    periodEnd: String
) derives Encoder.AsObject, Decoder

case class RunDetail(run: RunInfo, log: LogSummary, fraction: Option[Double]) derives Encoder.AsObject

/**
  * Starts simulations as separate processes and keeps their history on disk.
  *
  * Each run gets a folder holding its configuration, log and results, so a crash
  * or an out-of-memory error in a run cannot take the interface down, and a
  * cancel is just killing the process.
  *
  * @param command the command line that runs the simulator on a configuration file
  */
class RunManager(val runsDir: Path, val workDir: Path, command: Path => Seq[String], landFile: Option[Path] = None) {

  private val IdPattern = "^[0-9]{8}-[0-9]{6}-[A-Za-z0-9._-]+$".r
  private val Stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

  private class Entry(var info: RunInfo, var process: Option[Process], var cancelled: Boolean)
  private val entries = new ConcurrentHashMap[String, Entry]()

  loadHistory()

  def isValidId(id: String): Boolean = IdPattern.matches(id)

  def dirOf(id: String): Option[Path] =
    if (isValidId(id) && entries.containsKey(id)) Some(runsDir.resolve(id)) else None

  def list(): Seq[RunInfo] =
    entries.values().asScala.map(_.info).toSeq.sortBy(-_.startedAt)

  def get(id: String): Option[RunInfo] = Option(entries.get(id)).map(_.info)

  def isRunning: Boolean = entries.values().asScala.exists(_.info.status == "running")

  /** Starts a run of the given configuration text, unless one is already going. */
  def start(configName: String, text: String): Either[String, RunInfo] = synchronized {
    if (isRunning) Left("A run is already in progress.")
    else
      new ConfigurationFile().parse(text) match {
        case Left(failure) => Left(s"The configuration is not valid: ${failure.getMessage}")
        case Right(config) =>
          val base = configName.replaceAll("\\.ya?ml$", "").replaceAll("[^A-Za-z0-9._-]", "_")
          val id = uniqueId(s"${LocalDateTime.now().format(Stamp)}-$base")
          val runDir = runsDir.resolve(id)
          Files.createDirectories(runDir.resolve("output"))
          Files.writeString(runDir.resolve("source.yaml"), text)
          deriveConfig(text, runDir.resolve("output").toAbsolutePath) match {
            case Left(problem) => Left(problem)
            case Right(derived) =>
              Files.writeString(runDir.resolve("config.yaml"), derived)
              val info = RunInfo(
                id, configName, "running", System.currentTimeMillis(), None, None, None,
                config.flow.period.start, config.flow.period.end
              )
              launch(info, runDir)
          }
      }
  }

  def cancel(id: String): Boolean = Option(entries.get(id)) match {
    case Some(entry) if entry.info.status == "running" =>
      entry.cancelled = true
      entry.process.foreach { p =>
        p.destroy()
        // a process that ignores the polite request is killed after a few seconds
        val watchdog = new Thread(() => {
          Try(Thread.sleep(5000))
          if (p.isAlive) p.destroyForcibly()
        })
        watchdog.setDaemon(true)
        watchdog.start()
      }
      true
    case _ => false
  }

  /** The log from a byte offset on, and the offset to ask for next. */
  def log(id: String, from: Long): Option[(String, Long)] =
    dirOf(id).map { dir =>
      val file = dir.resolve("run.log")
      if (!Files.exists(file)) ("", 0L)
      else {
        val bytes = Files.readAllBytes(file)
        val start = math.min(math.max(from, 0L), bytes.length.toLong).toInt
        (new String(bytes, start, bytes.length - start, "UTF-8"), bytes.length.toLong)
      }
    }

  def detail(id: String): Option[RunDetail] =
    for {
      info <- get(id)
      dir <- dirOf(id)
    } yield {
      val text = Try(Files.readString(dir.resolve("run.log"))).getOrElse("")
      val summary = RunProgress.parse(text)
      RunDetail(info, summary, RunProgress.fraction(summary, info.periodStart, info.periodEnd))
    }

  def results(id: String): Option[Results] = dirOf(id).flatMap(ResultsReader.read)

  def map(id: String, trackLimit: Int): Option[RunMap] = dirOf(id).flatMap(MapReader.read(_, workDir, trackLimit, landFile))

  /** Removes a finished run and everything it wrote. A run still going cannot be deleted. */
  def delete(id: String): Either[String, Unit] = synchronized {
    Option(entries.get(id)) match {
      case None => Left("No such run.")
      case Some(entry) if entry.info.status == "running" => Left("Cancel the run before deleting it.")
      case Some(_) =>
        entries.remove(id)
        val dir = runsDir.resolve(id)
        if (Files.exists(dir)) {
          val walk = Files.walk(dir)
          try walk.sorted(java.util.Comparator.reverseOrder[Path]()).forEach(p => Files.deleteIfExists(p))
          finally walk.close()
        }
        Right(())
    }
  }

  /** Stops anything still running, when the interface shuts down. */
  def shutdown(): Unit =
    entries.values().asScala.foreach(e => e.process.filter(_.isAlive).foreach(_.destroy()))

  private def uniqueId(wanted: String): String =
    Iterator.from(0).map(n => if (n == 0) wanted else s"$wanted-$n").find(id => !entries.containsKey(id)).get

  // The simulator writes where the configuration says and logs to a file if told to.
  // A run wants everything inside its own folder and its log on the console, which
  // is captured below.
  private def deriveConfig(text: String, outputDir: Path): Either[String, String] =
    io.circe.yaml.parser.parse(text).left.map(_.getMessage).flatMap { json =>
      json.hcursor
        .downField("output")
        .withFocus(_.mapObject(_.add("saveOutputFilePath", Json.fromString(outputDir.toString)).add("logFile", Json.fromString(""))))
        .top
        .toRight("The configuration has no output section.")
        .map(io.circe.yaml.Printer.spaces2.pretty)
    }

  private def launch(info: RunInfo, runDir: Path): Either[String, RunInfo] =
    Try {
      val builder = new ProcessBuilder(command(runDir.resolve("config.yaml").toAbsolutePath)*)
        .directory(workDir.toFile)
        .redirectErrorStream(true)
        .redirectOutput(runDir.resolve("run.log").toFile)
      builder.start()
    }.toEither.left.map(e => s"Could not start the simulator: ${e.getMessage}").map { process =>
      val entry = new Entry(info, Some(process), false)
      entries.put(info.id, entry)
      save(entry.info)
      process.onExit().thenAccept(p => finish(entry, runDir, p.exitValue()))
      info
    }

  private def finish(entry: Entry, runDir: Path, exitCode: Int): Unit = {
    val summary = RunProgress.parse(Try(Files.readString(runDir.resolve("run.log"))).getOrElse(""))
    val hasResults = ResultsReader.outputDir(runDir).isDefined
    val (status, message) =
      if (entry.cancelled) ("cancelled", None)
      else if (exitCode == 0 && hasResults) ("succeeded", None)
      else ("failed", Some(summary.problems.headOption.getOrElse(if (exitCode != 0) s"The simulator exited with code $exitCode." else "The simulator wrote no results.")))
    entry.info = entry.info.copy(status = status, endedAt = Some(System.currentTimeMillis()), exitCode = Some(exitCode), message = message)
    save(entry.info)
  }

  private def save(info: RunInfo): Unit =
    Files.writeString(runsDir.resolve(info.id).resolve("run.json"), info.asJson.spaces2)

  // Runs left "running" by an earlier session were cut off with it
  private def loadHistory(): Unit =
    if (Files.isDirectory(runsDir)) {
      val dirs = Files.list(runsDir)
      try dirs.iterator().asScala.foreach { dir =>
        val file = dir.resolve("run.json")
        if (isValidId(dir.getFileName.toString) && Files.isRegularFile(file)) {
          // JSON is YAML, so the YAML parser (already a dependency) reads run.json too
          io.circe.yaml.parser.parse(Files.readString(file)).flatMap(_.as[RunInfo]).foreach { info =>
            val loaded =
              if (info.status == "running") info.copy(status = "interrupted", message = Some("The interface was closed while this run was going."))
              else info
            entries.put(loaded.id, new Entry(loaded, None, false))
          }
        }
      } finally dirs.close()
    }
}

object RunManager {

  /** Runs the simulator in a new JVM of the same kind, with the same classpath. */
  def jvmCommand(configFile: Path): Seq[String] = {
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toString
    val extra = Option(System.getenv("ZISSOU_JAVA_OPTS")).map(_.trim.split("\\s+").toSeq.filter(_.nonEmpty)).getOrElse(Seq.empty)
    Seq(java, "-XX:+UseParallelGC") ++ extra ++ Seq("-cp", System.getProperty("java.class.path"), "model.Simulator", configFile.toString)
  }
}

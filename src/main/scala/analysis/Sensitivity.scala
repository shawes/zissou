package analysis

import java.nio.file.{Files, Path}
import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import io.circe.Json
import scala.jdk.CollectionConverters.*
import scala.util.Try

/**
  * Runs the configurations of a sensitivity design, several at once, each in a process of its own, and sums up what each
  * came to in one table, `summary.csv`, a row for each run.
  *
  * Each run has a folder of its own in the design's output folder, holding its configuration, its log and its results.
  * A run that has results already is not run again, so a design that was stopped, or that has had runs added to it,
  * goes on with the runs it does not have.
  */
object Sensitivity {

  private val Usage =
    """Usage: zissou sensitivity DESIGN [--summary]
      |  Runs the configurations of a design (see analysis.Design for its form) and writes summary.csv in its output
      |  folder. Runs that have results already are not run again. With --summary it only writes the summary.
      |""".stripMargin

  def main(args: Array[String]): Unit = {
    val (flags, positional) = args.toList.partition(_.startsWith("--"))
    if (positional.size != 1 || (flags.toSet -- Set("--summary")).nonEmpty) {
      Console.err.print(Usage)
      sys.exit(2)
    }
    val workDir = Path.of("").toAbsolutePath
    val design = Design.read(Path.of(positional.head)).fold(fail, identity)
    val configs = configurations(design).fold(fail, identity)
    if (!flags.contains("--summary")) {
      val failed = run(design, configs, workDir)
      if (failed.nonEmpty) println(s"${failed.size} runs failed: ${failed.mkString(", ")}; see run.log in each one's folder.")
    }
    val summary = writeSummary(design, workDir)
    println(s"Wrote $summary")
  }

  private def fail(message: String): Nothing = {
    Console.err.println(message)
    sys.exit(1)
  }

  /** The folder of a run. */
  def dirOf(design: Design, run: PlannedRun): Path = design.output.resolve(run.name)

  /** The configuration of each run, as YAML: the baseline with the run's settings, writing to the run's folder. */
  def configurations(design: Design): Either[String, Seq[(PlannedRun, String)]] =
    io.circe.yaml.parser.parse(Files.readString(design.baseline)).left.map(e => s"${design.baseline} is not valid YAML: ${e.getMessage}")
      .flatMap { baseline =>
        val made = design.runs.map { run =>
          val output = dirOf(design, run).resolve("output").toAbsolutePath.toString
          val fixed = Seq(
            "output.saveOutputFilePath" -> Json.fromString(output),
            // the log goes to the console, which is kept in the run's folder
            "output.logFile" -> Json.fromString("")
          ) ++ design.flow.map(f => "flow.netcdfFilePath" -> Json.fromString(f))
          val settings = run.settings ++ fixed
          settings.foldLeft[Either[String, Json]](Right(baseline)) { case (acc, (path, value)) =>
            acc.flatMap(Paths.set(_, path, value))
          }.left.map(e => s"The run ${run.name}: $e").flatMap { json =>
            val text = io.circe.yaml.Printer.spaces2.pretty(json)
            new io.config.ConfigurationFile().parse(text).left
              .map(e => s"The run ${run.name} does not give a valid configuration: ${e.getMessage}")
              .map(_ => run -> text)
          }
        }
        made.collectFirst { case Left(e) => e }.toLeft(made.collect { case Right(r) => r })
      }

  /** Whether a run's folder has its results. */
  def isDone(dir: Path): Boolean =
    ui.ResultsReader.outputDir(dir).isDefined && Try(Files.readString(dir.resolve("run.log"))).toOption.exists(_.contains("Simulation run completed"))

  /** Runs the runs that have no results yet, `parallel` at a time, and returns the names of those that failed. */
  def run(design: Design, configs: Seq[(PlannedRun, String)], workDir: Path): Seq[String] = {
    val todo = configs.filterNot { case (run, _) => isDone(dirOf(design, run)) }
    println(s"${configs.size} runs in the design, ${configs.size - todo.size} done already, ${todo.size} to run, ${design.parallel} at a time.")
    if (todo.isEmpty) return Nil
    val concurrent = math.min(design.parallel, todo.size)
    val cores = model.Batch.coresPerRun(concurrent)
    val pool = Executors.newFixedThreadPool(concurrent)
    val finished = new AtomicInteger(0)
    try {
      val futures = todo.map { case (run, text) =>
        pool.submit(() => {
          val dir = dirOf(design, run)
          // what an earlier, unfinished go left is cleared, so that its results cannot be mistaken for this one's
          if (Files.exists(dir)) deleteTree(dir)
          Files.createDirectories(dir.resolve("output"))
          val config = dir.resolve("config.yaml")
          Files.writeString(config, text)
          val began = System.nanoTime
          val process = new ProcessBuilder(model.Batch.jvmCommand(config.toAbsolutePath, cores).asJava)
            .directory(workDir.toFile)
            .redirectErrorStream(true)
            .redirectOutput(dir.resolve("run.log").toFile)
            .start()
          val code = process.waitFor()
          val minutes = (System.nanoTime - began) / 6e10
          val ok = code == 0 && isDone(dir)
          println(f"[${finished.incrementAndGet()}/${todo.size}] ${run.name}: ${if (ok) "finished" else s"failed (exit code $code)"} in $minutes%.1f min")
          if (ok) None else Some(run.name)
        })
      }
      futures.flatMap(_.get)
    } finally {
      pool.shutdown()
      pool.awaitTermination(1, TimeUnit.SECONDS)
    }
  }

  private def deleteTree(dir: Path): Unit = {
    val walk = Files.walk(dir)
    try walk.sorted(java.util.Comparator.reverseOrder[Path]()).forEach(p => Files.delete(p))
    finally walk.close()
  }

  /** The baseline a run is compared with: the run named baseline with the same seed, or with none. */
  private def baselineOf(design: Design, run: PlannedRun): Option[PlannedRun] =
    design.runs.find(r => r.label == "baseline" && r.seed == run.seed)
      .orElse(design.runs.find(r => r.label == "baseline" && r.seed.isEmpty))

  /** Writes summary.csv, a row for each run of the design, and returns where. */
  def writeSummary(design: Design, workDir: Path): Path = {
    val metrics = design.runs.map(run => run -> RunMetrics.read(dirOf(design, run), workDir, design.regions)).toMap
    val paths = design.runs.flatMap(_.settings.map(_._1)).distinct.filterNot(_ == "settings.randomSeed")
    val sites = design.runs.flatMap(r => metrics(r).toSeq.flatMap(_.bySite.map(_._1))).distinct
    val header = Seq("run", "label", "seed", "status") ++ paths ++ Seq(
      "released", "settled", "settlement_pct", "median_km", "p90_km", "max_km", "mean_age_days",
      "self_recruitment_pct", s"retained_within_${RunMetrics.LocalKm.toInt}km_pct", "northward_pct"
    ) ++ design.regions.map(r => s"${r._1}_pct") ++ Seq("bray_curtis_vs_baseline", "baseline_links_kept_pct", "minutes") ++
      sites.flatMap(s => Seq(s"$s settled", s"$s median_km"))

    def pct(x: Double) = f"${x * 100}%.2f"
    def km(x: Double) = f"$x%.2f"
    val rows = design.runs.map { run =>
      val m = metrics(run)
      val status = if (m.isDefined) "done" else if (Files.exists(dirOf(design, run).resolve("run.log"))) "failed" else "not run"
      val values = paths.map(p => run.settings.find(_._1 == p).map(v => v._2.asString.getOrElse(v._2.noSpaces)).getOrElse(""))
      val base = baselineOf(design, run).flatMap(metrics(_))
      val outputs = m.toSeq.flatMap { m =>
        Seq(m.released.map(_.toString).getOrElse(""), m.settled.toString, m.settlement.map(pct).getOrElse(""),
          km(m.medianKm), km(m.p90Km), km(m.maxKm), f"${m.meanAgeDays}%.2f",
          pct(m.selfRecruitment), pct(m.localRetention), pct(m.northward)) ++
          m.regions.map(r => pct(r._2)) ++
          Seq(base.map(b => f"${m.brayCurtis(b)}%.4f").getOrElse(""), base.map(b => pct(m.linksKept(b))).getOrElse(""),
            m.minutes.map(x => f"$x%.1f").getOrElse("")) ++
          sites.flatMap(s => m.bySite.find(_._1 == s).map(x => Seq(x._2.toString, km(x._3))).getOrElse(Seq("0", "")))
      }
      Seq(run.name, run.label, run.seed.map(_.toString).getOrElse(""), status) ++ values ++
        (if (outputs.isEmpty) Seq.fill(header.size - 4 - paths.size)("") else outputs)
    }
    Files.createDirectories(design.output)
    val file = design.output.resolve("summary.csv")
    Files.write(file, (header +: rows).map(_.map(csvField).mkString(",")).asJava)
    file
  }

  private def csvField(s: String): String =
    if (s.exists(c => c == ',' || c == '"' || c == '\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s
}

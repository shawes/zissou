package model

import java.nio.file.Path
import java.util.concurrent.{Executors, TimeUnit}
import grizzled.slf4j.Logging
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*
import scala.util.Try

/**
  * Runs several configurations at once, each in a JVM of its own.
  *
  * A simulation moves its larvae on every core, so running several at once pays
  * for the parts that are not parallel (reading flow files, writing results) and
  * for a configuration too small to fill the machine, such as a seed or parameter
  * sweep. Each child is told it has only its share of the cores, so the pools in
  * all of them add up to the machine rather than each taking the lot.
  *
  * They must not write to the same output folder.
  */
object Batch extends Logging {

  /** The cores each of `concurrent` runs gets, at least one. */
  def coresPerRun(concurrent: Int, cores: Int = Runtime.getRuntime.availableProcessors): Int =
    math.max(1, cores / math.max(1, concurrent))

  /** The command that runs the simulator on a configuration file in a new JVM, with `cores` to itself. */
  def jvmCommand(configFile: Path, cores: Int): Seq[String] = {
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toString
    val extra = Option(System.getenv("ZISSOU_JAVA_OPTS")).map(_.trim.split("\\s+").toSeq.filter(_.nonEmpty)).getOrElse(Seq.empty)
    val limit = if (cores < Runtime.getRuntime.availableProcessors) Seq(s"-XX:ActiveProcessorCount=$cores") else Seq.empty
    Seq(java, "-XX:+UseParallelGC") ++ limit ++ extra ++
      Seq("-cp", System.getProperty("java.class.path"), "model.Simulator", configFile.toString)
  }

  /** Runs every configuration, `parallel` at a time, and returns the exit code of each in order. */
  def run(configs: Seq[String], parallel: Int): Seq[Int] = {
    val concurrent = math.max(1, math.min(parallel, configs.size))
    val cores = coresPerRun(concurrent)
    val pool = Executors.newFixedThreadPool(concurrent)
    try {
      val results = configs.map { config =>
        pool.submit(() => runOne(config, cores))
      }
      results.map(_.get)
    } finally {
      pool.shutdown()
      pool.awaitTermination(1, TimeUnit.SECONDS)
    }
  }

  private def runOne(config: String, cores: Int): Int = {
    val name = Path.of(config).getFileName.toString.replaceAll("\\.[^.]*$", "")
    val process = new ProcessBuilder(jvmCommand(Path.of(config).toAbsolutePath, cores).asJava)
      .redirectErrorStream(true)
      .start()
    // each line is prefixed with the configuration it came from, as the runs interleave
    val lines = scala.io.Source.fromInputStream(process.getInputStream)
    try lines.getLines().foreach(line => println(s"[$name] $line"))
    finally lines.close()
    val code = process.waitFor()
    println(s"[$name] ${if (code == 0) "finished" else s"failed with exit code $code"}")
    code
  }
}

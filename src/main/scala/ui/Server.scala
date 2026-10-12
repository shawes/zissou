package ui

import java.nio.file.{Files, Path}

/** Starts the local web interface: `scripts/zissou.sh ui` */
object Server {

  private val Usage =
    """Usage: zissou ui [--port 8787] [--host 127.0.0.1] [--configs DIR] [--runs DIR]
      |
      |  --port     the port to listen on (default 8787)
      |  --host     the address to listen on (default 127.0.0.1, this machine only).
      |             Use 0.0.0.0 to reach it from other machines; there is no login,
      |             so anyone who can reach the port can start runs and write files.
      |  --configs  the folder of configuration files (default: data/synthetic, else configs)
      |  --runs     where each run's log and results are kept (default: runs)
      |
      |Run it from the folder the configurations' relative paths refer to.
      |""".stripMargin

  def main(args: Array[String]): Unit = {
    if (args.contains("--help") || args.contains("-h")) {
      print(Usage)
    } else {
      val options = args.toSeq.grouped(2).collect { case Seq(k, v) if k.startsWith("--") => k.drop(2) -> v }.toMap
      val unknown = options.keySet -- Set("port", "host", "configs", "runs")
      if (unknown.nonEmpty || args.length % 2 != 0) {
        Console.err.print(Usage)
        sys.exit(2)
      }
      val workDir = Path.of("").toAbsolutePath
      val configDir = options.get("configs").map(Path.of(_)).getOrElse(
        if (Files.isDirectory(Path.of("data/synthetic"))) Path.of("data/synthetic") else Path.of("configs")
      )
      val runsDir = Path.of(options.getOrElse("runs", "runs"))
      val host = options.getOrElse("host", "127.0.0.1")
      val port = options.get("port").flatMap(_.toIntOption).getOrElse(8787)

      Files.createDirectories(runsDir)
      val runs = new RunManager(runsDir, workDir, RunManager.jvmCommand)
      val server = new WebServer(host, port, new ConfigStore(configDir, workDir), runs)
      server.start()
      Runtime.getRuntime.addShutdownHook(new Thread(() => {
        runs.shutdown()
        server.stop()
      }))
      println(s"Zissou is running at http://${if (host == "0.0.0.0") "localhost" else host}:${server.boundPort}/")
      println(s"  configurations: ${configDir.toAbsolutePath}")
      println(s"  runs:           ${runsDir.toAbsolutePath}")
      if (host != "127.0.0.1" && host != "localhost" && host != "::1") {
        println("  Listening beyond this machine: there is no login, so anyone who can reach this port can start runs.")
      }
      println("Press Ctrl+C to stop.")
      Thread.currentThread().join()
    }
  }
}

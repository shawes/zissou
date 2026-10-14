package ui

import java.net.{Socket, URI}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.{Files, Path}
import io.circe.Json
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

class WebServerTest extends AnyFlatSpec with BeforeAndAfterAll {

  private val succeeds =
    """d="$(dirname "$0")/output/config"; mkdir -p "$d"
      |echo '[main] INFO m - Day 2002-01-05 has been completed in 0 secs with 10 larvae.'
      |printf 'id,born,region,source,age,recruited-date,settle-id\na,d,patch 101,101,5.0,d,101\n' > "$d/x-connectivity-matrix.csv"
      |""".stripMargin

  private var base: Path = _
  private var runs: RunManager = _
  private var server: WebServer = _
  private var root: String = _
  private val client = HttpClient.newHttpClient()

  override def beforeAll(): Unit = {
    base = Files.createTempDirectory("zissou-web-test")
    val configs = new ConfigStore(base.resolve("configs"), base)
    configs.write("c.yaml", SampleConfig.yaml())
    configs.write("slow.yaml", SampleConfig.yaml())
    // the "slow" configuration is told apart by the script through its path
    runs = new RunManager(base.resolve("runs"), base, config =>
      Seq("sh", "-c", if (Files.readString(config.resolveSibling("source.yaml")).contains("SLOW")) "sleep 30" else succeeds, config.toString))
    server = new WebServer("127.0.0.1", 0, configs, runs)
    server.start()
    root = s"http://127.0.0.1:${server.boundPort}"
  }

  override def afterAll(): Unit = {
    runs.shutdown()
    server.stop()
  }

  private def get(path: String, headers: (String, String)*): HttpResponse[String] =
    send("GET", path, None, headers)

  private def send(method: String, path: String, body: Option[String], headers: Seq[(String, String)] = Seq.empty, json: Boolean = true): HttpResponse[String] = {
    val builder = HttpRequest.newBuilder(URI.create(root + path))
    if (json && method != "GET") builder.header("Content-Type", "application/json")
    headers.foreach { case (k, v) => builder.header(k, v) }
    builder.method(method, body.map(HttpRequest.BodyPublishers.ofString).getOrElse(HttpRequest.BodyPublishers.noBody()))
    client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
  }

  private def parse(response: HttpResponse[String]): Json = io.circe.yaml.parser.parse(response.body()).toOption.get
  private def text(json: Json, field: String) = json.hcursor.get[String](field).toOption.get
  private def jsonBody(fields: (String, String)*) = Json.obj(fields.map { case (k, v) => k -> Json.fromString(v) }*).noSpaces

  private def awaitDone(id: String): Unit = {
    val deadline = System.currentTimeMillis() + 15000
    while (System.currentTimeMillis() < deadline && runs.get(id).exists(_.status == "running")) Thread.sleep(25)
    assert(!runs.get(id).exists(_.status == "running"), s"run $id did not finish")
  }

  private def runAndWait(config: String): String = {
    val started = send("POST", "/api/runs", Some(jsonBody("config" -> config)))
    assert(started.statusCode() == 200, started.body())
    val id = text(parse(started), "id")
    awaitDone(id)
    id
  }

  "The page" should "be served with its script and styles" in {
    val page = get("/")
    assert(page.statusCode() == 200 && page.headers().firstValue("Content-Type").get.startsWith("text/html"))
    assert(page.body().contains("<title>Zissou</title>"))
    assert(get("/app.js").headers().firstValue("Content-Type").get.startsWith("text/javascript"))
    assert(get("/style.css").statusCode() == 200)
  }

  it should "serve the form, the map picker and the code editor the page loads" in {
    val page = get("/").body()
    for (script <- Seq("/form.js", "/picker.js", "/vendor/editor.js")) {
      assert(page.contains(s"""src="$script""""), script)
      val response = get(script)
      assert(response.statusCode() == 200 && response.headers().firstValue("Content-Type").get.startsWith("text/javascript"), script)
      assert(response.body().nonEmpty, script)
    }
  }

  "The land endpoint" should "answer with the land in a box, none when there is no land layer" in {
    val response = get("/api/land?west=148&south=-40&east=154&north=-30")
    assert(response.statusCode() == 200)
    assert(parse(response).hcursor.downField("land").as[Seq[Json]].toOption.exists(_.isEmpty))
  }

  it should "refuse a box that is not one" in {
    for (query <- Seq("", "west=1&south=1&east=2", "west=2&south=1&east=1&north=2", "west=a&south=1&east=2&north=2", "west=0&south=0&east=500&north=10"))
      assert(get(s"/api/land?$query").statusCode() == 400, query)
  }

  it should "send headers that stop caching and content sniffing" in {
    val page = get("/")
    assert(page.headers().firstValue("Cache-Control").get == "no-store")
    assert(page.headers().firstValue("X-Content-Type-Options").get == "nosniff")
  }

  it should "answer 404 for anything else" in {
    assert(get("/nope").statusCode() == 404)
    assert(get("/api/nope").statusCode() == 404)
    assert(get("/index.html").statusCode() == 404)
    assert(get("/../etc/passwd").statusCode() == 404)
  }

  "The configuration API" should "list, save, read and check configurations" in {
    assert(parse(get("/api/configs")).asArray.get.map(c => text(c, "name")).toSet == Set("c.yaml", "slow.yaml"))
    val saved = send("PUT", "/api/configs/new.yaml", Some(jsonBody("text" -> SampleConfig.yaml(interval = 1))))
    assert(saved.statusCode() == 200, saved.body())
    assert(text(parse(get("/api/configs/new.yaml")), "text") == SampleConfig.yaml(interval = 1))
    val check = send("POST", "/api/validate", Some(jsonBody("text" -> SampleConfig.yaml())))
    assert(parse(check).hcursor.get[Boolean]("ok").contains(true))
  }

  it should "refuse to save a configuration that is not valid" in {
    val rejected = send("PUT", "/api/configs/bad.yaml", Some(jsonBody("text" -> "settings: [")))
    assert(rejected.statusCode() == 400 && get("/api/configs/bad.yaml").statusCode() == 404)
  }

  it should "refuse names that could leave the folder" in {
    assert(send("PUT", "/api/configs/..%2Fescape.yaml", Some(jsonBody("text" -> SampleConfig.yaml()))).statusCode() == 400)
    assert(get("/api/configs/..%2F..%2Fetc%2Fpasswd").statusCode() == 404)
    assert(!Files.exists(base.resolve("escape.yaml")))
  }

  "The run API" should "run a configuration and report its results" in {
    val id = runAndWait("c.yaml")
    val detail = parse(get(s"/api/runs/$id"))
    assert(text(detail.hcursor.downField("run").focus.get, "status") == "succeeded")
    assert(parse(get(s"/api/runs/$id/results")).hcursor.get[Int]("settled").contains(1))
    assert(get(s"/api/runs/$id/log").body().contains("Day 2002-01-05"))
    assert(parse(get("/api/runs")).asArray.get.map(r => text(r, "id")).contains(id))
  }

  it should "run the text the page sends, not only the saved file" in {
    val started = send("POST", "/api/runs", Some(jsonBody("config" -> "c.yaml", "text" -> SampleConfig.yaml(releaseEnd = "2002-01-25"))))
    assert(started.statusCode() == 200, started.body())
    val id = text(parse(started), "id")
    awaitDone(id)
    assert(Files.readString(base.resolve("runs").resolve(id).resolve("source.yaml")).contains("end: 2002-01-25"))
  }

  it should "serve a map for a finished run, and 404 for an unknown one" in {
    val id = runAndWait("c.yaml")
    val response = get(s"/api/runs/$id/map?tracks=abc")
    assert(response.statusCode() == 200, response.body())
    val body = parse(response)
    assert(body.hcursor.downField("sites").values.exists(_.size == 1))
    assert(body.hcursor.downField("notes").values.exists(_.nonEmpty), "the fake run wrote no habitat shapefile, which it says")
    assert(get(s"/api/runs/$id/map?tracks=-5").statusCode() == 200 && get(s"/api/runs/$id/map?tracks=999999").statusCode() == 200)
    assert(get("/api/runs/20000101-000000-none/map").statusCode() == 404)
  }

  it should "serve result files, and only result files" in {
    val id = runAndWait("c.yaml")
    assert(get(s"/api/runs/$id/files/x-connectivity-matrix.csv").body().startsWith("id,born"))
    assert(get(s"/api/runs/$id/files/..%2Fconfig.yaml").statusCode() == 404)
    assert(get(s"/api/runs/$id/files/..%2F..%2Frun.log").statusCode() == 404)
    assert(get(s"/api/runs/$id/files/missing.csv").statusCode() == 404)
  }

  it should "answer 404 for unknown runs and bad ids" in {
    assert(get("/api/runs/20000101-000000-none").statusCode() == 404)
    assert(get("/api/runs/..%2F..").statusCode() == 404)
    assert(send("POST", "/api/runs", Some(jsonBody("config" -> "missing.yaml"))).statusCode() == 404)
    assert(send("POST", "/api/runs", Some(jsonBody("config" -> "../c.yaml"))).statusCode() == 400)
    assert(send("POST", "/api/runs", Some("{}")).statusCode() == 400)
  }

  it should "allow one run at a time, and cancel it on request" in {
    val slowText = SampleConfig.yaml() + "# SLOW\n"
    val first = send("POST", "/api/runs", Some(jsonBody("config" -> "slow.yaml", "text" -> slowText)))
    assert(first.statusCode() == 200, first.body())
    val id = text(parse(first), "id")
    assert(send("POST", "/api/runs", Some(jsonBody("config" -> "c.yaml"))).statusCode() == 409)
    assert(send("DELETE", s"/api/runs/$id", None).statusCode() == 409, "a run that is going cannot be deleted")
    assert(send("POST", s"/api/runs/$id/cancel", Some("{}")).statusCode() == 200)
    awaitDone(id)
    assert(runs.get(id).get.status == "cancelled")
    assert(send("POST", s"/api/runs/$id/cancel", Some("{}")).statusCode() == 409)
  }

  it should "delete a finished run" in {
    val id = runAndWait("c.yaml")
    assert(send("DELETE", s"/api/runs/$id", None).statusCode() == 200)
    assert(get(s"/api/runs/$id").statusCode() == 404 && !Files.exists(base.resolve("runs").resolve(id)))
  }

  it should "archive a finished run and bring it back" in {
    val id = runAndWait("c.yaml")
    assert(send("POST", s"/api/runs/$id/archive", Some("{}")).statusCode() == 400)
    assert(send("POST", s"/api/runs/$id/archive", Some("""{"archived":true}""")).statusCode() == 200)
    assert(runs.get(id).get.archived.contains(true))
    assert(send("POST", s"/api/runs/$id/archive", Some("""{"archived":false}""")).statusCode() == 200)
    assert(runs.get(id).get.archived.isEmpty)
    assert(send("POST", "/api/runs/20200101-000000-none/archive", Some("""{"archived":true}""")).statusCode() == 404)
  }

  "The server" should "refuse a request that is not JSON, which a web form could send" in {
    val response = send("POST", "/api/validate", Some("text=x"), Seq("Content-Type" -> "application/x-www-form-urlencoded"), json = false)
    assert(response.statusCode() == 415)
    assert(send("POST", "/api/validate", Some("{}"), json = false).statusCode() == 415)
  }

  it should "refuse a request from another website" in {
    val response = send("POST", "/api/validate", Some(jsonBody("text" -> "x")), Seq("Origin" -> "http://evil.example"))
    assert(response.statusCode() == 403)
    val same = send("POST", "/api/validate", Some(jsonBody("text" -> "x")), Seq("Origin" -> s"http://127.0.0.1:${server.boundPort}"))
    assert(same.statusCode() == 200)
  }

  it should "refuse a request whose Host is not this machine, which is how DNS rebinding looks" in {
    val socket = new Socket("127.0.0.1", server.boundPort)
    try {
      socket.getOutputStream.write("GET /api/configs HTTP/1.1\r\nHost: evil.example\r\nConnection: close\r\n\r\n".getBytes("UTF-8"))
      socket.getOutputStream.flush()
      val reply = new String(socket.getInputStream.readAllBytes(), "UTF-8")
      assert(reply.startsWith("HTTP/1.1 403"), reply.take(80))
    } finally socket.close()
  }

  it should "refuse a request that is too large" in {
    val big = "x" * (1024 * 1024 + 1)
    assert(send("POST", "/api/validate", Some(big)).statusCode() == 413)
  }
}

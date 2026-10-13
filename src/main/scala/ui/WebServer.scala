package ui

import java.net.{InetSocketAddress, URI, URLDecoder}
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.concurrent.Executors
import com.sun.net.httpserver.{HttpExchange, HttpServer}
import io.circe.{Encoder, Json}
import io.circe.syntax.*
import scala.util.Try

private case class Request(method: String, path: Seq[String], query: Map[String, String], body: String)
private case class Response(status: Int, contentType: String, body: Array[Byte])

/**
  * A small JSON API and the page that uses it, on the JDK's built-in HTTP server.
  *
  * It can start processes and write files, so it listens on this machine only by
  * default, and refuses requests that did not come from its own page.
  */
class WebServer(host: String, port: Int, configs: ConfigStore, runs: RunManager) {

  private val MaxBody = 1024 * 1024
  private val server = HttpServer.create(new InetSocketAddress(host, port), 0)
  private val loopback = Set("localhost", "127.0.0.1", "[::1]")

  private val assets = Map(
    "/" -> ("index.html", "text/html; charset=utf-8"),
    "/app.js" -> ("app.js", "text/javascript; charset=utf-8"),
    "/form.js" -> ("form.js", "text/javascript; charset=utf-8"),
    "/picker.js" -> ("picker.js", "text/javascript; charset=utf-8"),
    "/vendor/editor.js" -> ("vendor/editor.js", "text/javascript; charset=utf-8"),
    "/style.css" -> ("style.css", "text/css; charset=utf-8")
  )

  server.createContext("/", exchange => handle(exchange))
  server.setExecutor(Executors.newCachedThreadPool { r =>
    val t = new Thread(r, "zissou-ui")
    t.setDaemon(true)
    t
  })

  def start(): Unit = server.start()
  def stop(): Unit = server.stop(0)
  def boundPort: Int = server.getAddress.getPort

  private def handle(exchange: HttpExchange): Unit = {
    val response =
      try {
        val request = read(exchange)
        refusal(exchange, request).getOrElse(route(request))
      } catch {
        case _: PayloadTooLarge => error(413, "That request is too large.")
        case e: Throwable => error(500, s"Something went wrong: ${e.getMessage}")
      }
    exchange.getResponseHeaders.set("Content-Type", response.contentType)
    exchange.getResponseHeaders.set("Cache-Control", "no-store")
    exchange.getResponseHeaders.set("X-Content-Type-Options", "nosniff")
    exchange.sendResponseHeaders(response.status, if (response.body.isEmpty) -1 else response.body.length.toLong)
    if (response.body.nonEmpty) exchange.getResponseBody.write(response.body)
    exchange.close()
  }

  private class PayloadTooLarge extends Exception

  private def read(exchange: HttpExchange): Request = {
    val uri: URI = exchange.getRequestURI
    val query = Option(uri.getRawQuery).toSeq.flatMap(_.split("&")).filter(_.nonEmpty).map { pair =>
      val Array(k, v) = (pair + "=").split("=", 3).take(2)
      URLDecoder.decode(k, UTF_8) -> URLDecoder.decode(v, UTF_8)
    }.toMap
    val body = {
      val bytes = exchange.getRequestBody.readNBytes(MaxBody + 1)
      if (bytes.length > MaxBody) throw new PayloadTooLarge
      new String(bytes, UTF_8)
    }
    // getPath has already decoded %2F, which would split one name into two, and decoding
    // twice would turn %252F into a slash; take the raw path and decode each piece once
    val segments = uri.getRawPath.split("/").filter(_.nonEmpty)
      .map(piece => URLDecoder.decode(piece.replace("+", "%2B"), UTF_8)).toSeq
    Request(exchange.getRequestMethod, segments, query, body)
  }

  // DNS rebinding and cross-site requests are how a web page could reach a local
  // server, so check who is asking before doing anything
  private def refusal(exchange: HttpExchange, request: Request): Option[Response] = {
    val headers = exchange.getRequestHeaders
    val hostHeader = Option(headers.getFirst("Host")).getOrElse("")
    val hostName = hostHeader.replaceFirst(":\\d+$", "")
    if (loopback.contains(host) && !loopback.contains(hostName)) Some(error(403, "Unexpected Host header."))
    else if (request.method != "GET" && request.method != "HEAD") {
      val contentType = Option(headers.getFirst("Content-Type")).getOrElse("")
      val origin = Option(headers.getFirst("Origin"))
      if (!contentType.startsWith("application/json")) Some(error(415, "Requests must be application/json."))
      else if (origin.exists(o => o.replaceFirst("^https?://", "") != hostHeader)) Some(error(403, "Cross-origin requests are not allowed."))
      else None
    } else None
  }

  private def route(request: Request): Response = {
    val path = "/" + request.path.mkString("/")
    (request.method, request.path.toList) match {
      case ("GET", _) if assets.contains(path) => asset(path)
      case ("GET", List("api", "info")) =>
        json(200, Json.obj(
          "configDir" -> configs.dir.toString.asJson,
          "runsDir" -> runs.runsDir.toString.asJson,
          "running" -> runs.isRunning.asJson,
          "runningCount" -> runs.runningCount.asJson,
          "maxConcurrent" -> runs.maxConcurrent.asJson
        ))
      case ("GET", List("api", "configs")) => json(200, configs.list().asJson)
      case ("GET", List("api", "configs", name)) =>
        configs.read(name) match {
          case Some(text) => json(200, Json.obj("name" -> name.asJson, "text" -> text.asJson))
          case None => error(404, "No such configuration.")
        }
      case ("PUT", List("api", "configs", name)) => saveConfig(name, request)
      case ("POST", List("api", "validate")) =>
        bodyText(request, "text").fold(error(400, _), text => json(200, configs.validate(text).asJson))
      case ("GET", List("api", "land")) => land(request)
      case ("GET", List("api", "runs")) => json(200, runs.list().asJson)
      case ("POST", List("api", "runs")) => startRun(request)
      case ("GET", List("api", "runs", id)) => orNotFound(runs.detail(id))(d => json(200, d.asJson))
      case ("DELETE", List("api", "runs", id)) =>
        runs.delete(id).fold(m => error(409, m), _ => json(200, Json.obj("deleted" -> id.asJson)))
      case ("GET", List("api", "runs", id, "log")) =>
        val from = request.query.get("from").flatMap(s => Try(s.toLong).toOption).getOrElse(0L)
        orNotFound(runs.log(id, from)) { case (text, next) => json(200, Json.obj("text" -> text.asJson, "next" -> next.asJson)) }
      case ("POST", List("api", "runs", id, "cancel")) =>
        if (runs.cancel(id)) json(200, Json.obj("cancelling" -> id.asJson)) else error(409, "That run is not in progress.")
      case ("GET", List("api", "runs", id, "results")) =>
        orNotFound(runs.results(id))(r => json(200, r.asJson))
      case ("GET", List("api", "runs", id, "map")) =>
        val limit = request.query.get("tracks").flatMap(_.toIntOption).getOrElse(400).max(1).min(2000)
        orNotFound(runs.map(id, limit))(m => json(200, m.asJson))
      case ("GET", List("api", "runs", id, "files", name)) => download(id, name)
      case _ => error(404, "Not found.")
    }
  }

  // the coastline inside a box, for the map where a site or a flow region is chosen
  private def land(request: Request): Response = {
    val box = Seq("west", "south", "east", "north").map(k => request.query.get(k).flatMap(_.toDoubleOption).filter(!_.isNaN))
    box.flatten match {
      case Seq(west, south, east, north) if box.forall(_.isDefined) && west < east && south < north && east - west <= 360 && north - south <= 180 =>
        json(200, Json.obj("land" -> runs.land(Seq(west, south, east, north)).asJson))
      case _ => error(400, "Give west, south, east and north, with west less than east and south less than north.")
    }
  }

  private def saveConfig(name: String, request: Request): Response =
    if (!configs.isValidName(name)) error(400, "A configuration name may use letters, digits, '.', '-' and '_', and must end in .yaml or .yml.")
    else
      bodyText(request, "text").fold(error(400, _), { text =>
        val check = configs.validate(text)
        if (!check.ok) json(400, Json.obj("error" -> "The configuration is not valid.".asJson, "validation" -> check.asJson))
        else {
          configs.write(name, text)
          json(200, Json.obj("saved" -> name.asJson, "validation" -> check.asJson))
        }
      })

  private def startRun(request: Request): Response = {
    val fields = parse(request.body)
    val config = fields.flatMap(_.hcursor.get[String]("config").toOption)
    config match {
      case None => error(400, "Say which configuration to run.")
      case Some(name) if !configs.isValidName(name) => error(400, "Not a valid configuration name.")
      case Some(name) =>
        // the text the page is showing can differ from the saved file
        val text = fields.flatMap(_.hcursor.get[String]("text").toOption).orElse(configs.read(name))
        text match {
          case None => error(404, "No such configuration.")
          case Some(t) =>
            runs.start(name, t).fold(m => error(409, m), info => json(200, info.asJson))
        }
    }
  }

  private def download(id: String, name: String): Response =
    runs.dirOf(id).flatMap(ResultsReader.outputDir).filter(_ => name.matches("^[A-Za-z0-9][A-Za-z0-9._-]*$")) match {
      case Some(dir) if Files.isRegularFile(dir.resolve(name)) =>
        Response(200, "application/octet-stream", Files.readAllBytes(dir.resolve(name)))
      case _ => error(404, "No such file.")
    }

  private def asset(path: String): Response = {
    val (file, contentType) = assets(path)
    Option(getClass.getResourceAsStream(s"/ui/$file")) match {
      case Some(stream) => try Response(200, contentType, stream.readAllBytes()) finally stream.close()
      case None => error(404, s"Missing page resource $file.")
    }
  }

  private def parse(text: String): Option[Json] = io.circe.yaml.parser.parse(text).toOption

  private def bodyText(request: Request, field: String): Either[String, String] =
    parse(request.body).flatMap(_.hcursor.get[String](field).toOption).toRight(s"The request needs a '$field' field.")

  private def orNotFound[A](value: Option[A])(f: A => Response): Response =
    value.map(f).getOrElse(error(404, "Not found."))

  private def json(status: Int, value: Json): Response =
    Response(status, "application/json; charset=utf-8", value.noSpaces.getBytes(UTF_8))

  private def error(status: Int, message: String): Response =
    json(status, Json.obj("error" -> message.asJson))
}

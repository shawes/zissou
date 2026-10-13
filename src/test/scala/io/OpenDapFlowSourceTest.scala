package io

import java.net.InetSocketAddress
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger
import com.sun.net.httpserver.{HttpExchange, HttpServer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

/** The parts of reading from an OPeNDAP server that do not need the data: which months it has, and failing. */
class OpenDapFlowSourceTest extends AnyFlatSpec with BeforeAndAfterAll {

  // answers for the structure (.dds) of the files of January to March 2002, like THREDDS does: 200, or 404 for a
  // file that is not there; the first `failures` requests are answered with a 500, a server having trouble
  private val months = Set("2002_01", "2002_02", "2002_03")
  private val requests = new AtomicInteger(0)
  @volatile private var failures = 0
  private val paths = new java.util.concurrent.ConcurrentLinkedQueue[String]()

  private val server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
  server.createContext("/thredds/dodsC/daily", (exchange: HttpExchange) => {
    requests.incrementAndGet()
    paths.add(exchange.getRequestURI.getPath)
    val path = exchange.getRequestURI.getPath
    val status =
      if (failures > 0) { failures -= 1; 500 }
      else if (path.endsWith(".nc.dds") && months.exists(month => path.endsWith(s"_$month.nc.dds"))) 200
      else 404
    exchange.sendResponseHeaders(status, -1)
    exchange.close()
  })
  private def address = s"http://localhost:${server.getAddress.getPort}/thredds/dodsC/daily"

  override def beforeAll(): Unit = server.start()
  override def afterAll(): Unit = server.stop(0)

  private def source(base: String = address, attempts: Int = 3) = new OpenDapFlowSource(base, "ocean", List("u", "v", "w"), attempts, 1)
  private def month(year: Int, m: Int) = YearMonth.of(year, m)

  "The address of a month's file" should "be the directory, the prefix, the variable, the year and the month" in {
    assert(source("https://example.org/dodsC/daily/").url("u", month(2019, 1)) == "https://example.org/dodsC/daily/ocean_u_2019_01.nc")
    assert(new OpenDapFlowSource("https://example.org/dodsC/daily", "bran", List("u"), 1, 1).url("w", month(2019, 12)) ==
      "https://example.org/dodsC/daily/bran_w_2019_12.nc")
  }

  "A flow path" should "be an address when it starts with http:// or https://, and a folder otherwise" in {
    assert(OpenDapFlowSource.isUrl("https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily"))
    assert(OpenDapFlowSource.isUrl("http://localhost:8080/dodsC"))
    assert(!OpenDapFlowSource.isUrl("/data/flow") && !OpenDapFlowSource.isUrl("flow") && !OpenDapFlowSource.isUrl("C:\\flow"))
    assert(FlowFileIterator.sourceFor(new physical.flow.Flow("https://example.org/dodsC", null, null, true)).isInstanceOf[OpenDapFlowSource])
  }

  "A source on a server" should "have the months the server has the files for, and nothing after the first it has not" in {
    val s = source()
    assert(s.has(month(2002, 1)) && s.has(month(2002, 3)))
    assert(!s.has(month(2002, 4)) && !s.has(month(2001, 12)))
    assert(s.firstAfter(month(2002, 1)).contains(month(2002, 2)))
    assert(s.firstAfter(month(2002, 3)).isEmpty)
    assert(s.firstAfter(month(2002, 12)).isEmpty)
    assert(s.isRemote && s.description == address)
  }

  it should "ask for all three variables' files, and ask once for a month" in {
    requests.set(0)
    paths.clear()
    val s = source()
    assert(s.has(month(2002, 2)) && s.has(month(2002, 2)) && s.has(month(2002, 2)))
    assert(requests.get == 3)
    assert(paths.toArray.toSeq.map(_.toString).sorted == Seq("ocean_u", "ocean_v", "ocean_w").map(n => s"/thredds/dodsC/daily/${n}_2002_02.nc.dds"))
  }

  it should "try a request again when the server has trouble, and carry on" in {
    failures = 2
    assert(source().has(month(2002, 1)))
    assert(failures == 0)
  }

  it should "fail with the trouble when it does not clear, not say the month is not there" in {
    failures = 100
    try {
      val problem = intercept[java.io.IOException](source(attempts = 3).has(month(2002, 1)))
      assert(problem.getMessage.contains("answered 500"), problem.getMessage)
    } finally failures = 0
  }

  it should "fail, not say a month is not there, when the server does not answer" in {
    // a server that takes the request and never replies
    val silent = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
    silent.createContext("/dodsC", (exchange: HttpExchange) => Thread.sleep(5000))
    silent.start()
    try {
      val unresponsive = new OpenDapFlowSource(s"http://localhost:${silent.getAddress.getPort}/dodsC", "ocean", List("u", "v", "w"), 2, 1, timeoutSeconds = 1)
      val start = System.nanoTime
      intercept[java.io.IOException](unresponsive.has(month(2002, 1)))
      assert((System.nanoTime - start) / 1e9 < 4, "gave up after the timeout, twice")
    } finally silent.stop(0)
  }

  "Retrying" should "try again only on a failure to read, and give up after the attempts" in {
    val s = source(attempts = 3)
    var tries = 0
    assert(s.retrying("a read") { tries += 1; if (tries < 3) throw new java.io.IOException("reset") else "data" } == "data")
    assert(tries == 3)
    var more = 0
    intercept[java.io.IOException](s.retrying("a read") { more += 1; throw new java.io.IOException("reset") })
    assert(more == 3)
    intercept[IllegalStateException](s.retrying("a read") { throw new IllegalStateException("not a read problem") })
  }
}

package io

import java.nio.file.{Files, Path}
import java.time.YearMonth
import scala.util.Random
import com.github.nscala_time.time.Imports._
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.flow.{Flow, FlowController, FlowRegion}
import physical.{GeoCoordinate, TimeStep}
import tools.{StaggeredFlowDataset, UniformFlowDataset}

class FlowCacheTest extends AnyFlatSpec {

  private val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
  private val time = new LocalDateTime(2002, 1, 6, 9, 0)
  private val months = Seq(YearMonth.of(2002, 1), YearMonth.of(2002, 2))
  private val region = FlowRegion(-28.3, -27.7, 153.2, 153.8)

  private def cached(write: java.io.File => Unit, levels: Int = 10, small: Boolean = false): (Path, Path, FlowCache) = {
    val original = Files.createTempDirectory("zissou-cache-source")
    write(original.toFile)
    val copy = Files.createTempDirectory("zissou-cache-copy")
    val cache = new FlowCache(new LocalFlowSource(original.toString, FlowFileIterator.Variables), FlowFileIterator.Variables,
      if (small) FlowRegion(-28.1, -27.9, 153.4, 153.6) else region, levels, "ocean")
    cache.copy(months, copy)
    (original, copy, cache)
  }

  private def controller(root: Path, region: FlowRegion, levels: Int): (FlowController, FlowFileIterator) = {
    val flow = new Flow(root.toString, start to start.plusDays(20), new TimeStep(2, TimeStepType.Hour), true, region, levels)
    val iterator = new FlowFileIterator(flow)
    val c = new FlowController(iterator, flow)
    c.advanceTo(time)
    (c, iterator)
  }

  "A copy of the flow" should "give the same flow as the original, inside its region" in {
    val (original, copy, _) = cached(StaggeredFlowDataset.write(_, Seq((2002, 1), (2002, 2)), curvature = 0.3))
    val (fromOriginal, a) = controller(original, region, 10)
    val (fromCopy, b) = controller(copy, region, 10)
    try {
      val random = new Random(3)
      for (_ <- 1 to 200) {
        val point = new GeoCoordinate(-28.2 + random.nextDouble() * 0.4, 153.3 + random.nextDouble() * 0.4, random.nextDouble() * 40)
        val expected = fromOriginal.getVelocityOfCoordinate(point, time).get
        val actual = fromCopy.getVelocityOfCoordinate(point, time).get
        assert(math.abs(actual.u - expected.u) < 1e-9 && math.abs(actual.v - expected.v) < 1e-9 && math.abs(actual.w - expected.w) < 1e-12,
          s"at $point: $actual, not $expected")
      }
    } finally { a.shutdown(); b.shutdown() }
  }

  it should "hold only the region and levels asked for, with a margin round it, and every day of each month" in {
    val (_, copy, cache) = cached(StaggeredFlowDataset.write(_, Seq((2002, 1), (2002, 2))), levels = 6, small = true)
    val file = ucar.nc2.NetcdfFiles.open(cache.fileOf(copy, "u", YearMonth.of(2002, 2)).toString)
    try {
      val u = file.findVariable("u")
      val Array(days, levels, rows, columns) = u.getShape
      assert(days == 28 && levels == 6)
      val lats = file.findVariable("yu_ocean").read().copyTo1DJavaArray().asInstanceOf[Array[Double]]
      val lons = file.findVariable("xu_ocean").read().copyTo1DJavaArray().asInstanceOf[Array[Double]]
      assert(lats.length == rows && lons.length == columns)
      // the nodes round the region, and the margin, and no more
      val margin = FlowCache.Margin * 0.1 + 0.001
      assert(lats.min <= -28.1 && lats.max >= -27.9 && lats.min >= -28.1 - margin && lats.max <= -27.9 + margin, lats.mkString(", "))
      assert(lons.min <= 153.4 && lons.max >= 153.6 && lons.min >= 153.4 - margin && lons.max <= 153.6 + margin, lons.mkString(", "))
      assert(lats.length < 11 && lons.length < 11, "less than the whole grid")
    } finally file.close()
  }

  it should "keep values packed in 16 bits packed, with their scale factor" in {
    val scale = 3.0518509e-4
    val (original, copy, cache) = cached(UniformFlowDataset.write(_, Seq((2002, 1), (2002, 2)), Some(scale)), levels = 5)
    val file = ucar.nc2.NetcdfFiles.open(cache.fileOf(copy, "u", YearMonth.of(2002, 1)).toString)
    try {
      val u = file.findVariable("u")
      assert(u.getDataType == ucar.ma2.DataType.SHORT)
      assert(math.abs(u.findAttribute("scale_factor").getNumericValue.doubleValue - scale) < 1e-9, "stored in 32 bits")
    } finally file.close()
    val whole = FlowRegion(-28.5, -27.5, 153.0, 154.0)
    val (fromOriginal, a) = controller(original, whole, 5)
    val (fromCopy, b) = controller(copy, whole, 5)
    try {
      val point = new GeoCoordinate(-28.0, 153.5, 5.0)
      val expected = fromOriginal.getVelocityOfCoordinate(point, time).get
      val actual = fromCopy.getVelocityOfCoordinate(point, time).get
      assert(math.abs(actual.u - expected.u) < 1e-9 && math.abs(actual.u) > 0.01, s"$actual, not $expected")
    } finally { a.shutdown(); b.shutdown() }
  }

  it should "go on from where a stopped copy left off, and say how big it would be without reading the values" in {
    val (_, copy, cache) = cached(StaggeredFlowDataset.write(_, Seq((2002, 1), (2002, 2))))
    val written = cache.fileOf(copy, "v", YearMonth.of(2002, 2))
    val before = Files.getLastModifiedTime(written)
    Files.delete(cache.fileOf(copy, "w", YearMonth.of(2002, 2)))
    assert(cache.copy(months, copy) == Seq(cache.fileOf(copy, "w", YearMonth.of(2002, 2))))
    assert(Files.getLastModifiedTime(written) == before)
    val parts = cache.plan(months)
    assert(parts.size == 6)
    val january = parts.find(p => p.variable == "u" && p.month == YearMonth.of(2002, 1)).get
    assert(january.days == 31 && january.levels == 10 && january.bytesPerValue == 4)
    assert(january.bytes == 31L * 10 * january.rows * january.columns * 4)
  }
}

package physical.habitat

import locals.Reef
import org.scalatest.flatspec.AnyFlatSpec
import physical.GeoCoordinate
import tools.ShapefileTestUtil

class GeometryAdaptorTest extends AnyFlatSpec {

  // a square of 0.1 degrees, centred on (153.5, -28.0)
  private val reef = new GeometryAdaptor(
    ShapefileTestUtil.multiPolygon(Seq(Seq((153.45, -28.05), (153.45, -27.95), (153.55, -27.95), (153.55, -28.05)))), 9, Reef)

  "The distance to a reef" should "be zero on the reef" in {
    assert(reef.distance(new GeoCoordinate(-28.0, 153.5, 5.0)) == 0.0)
  }

  it should "be the distance in kilometres to the nearest point of the reef" in {
    // 0.1 degrees of latitude north of the northern edge is about 11.1 km
    val km = reef.distance(new GeoCoordinate(-27.85, 153.5, 5.0))
    assert(math.abs(km - 11.1) < 0.2, km)
  }

  "A reef" should "hold a point on it however small the buffer, including none" in {
    val on = new GeoCoordinate(-28.0, 153.5, 5.0)
    assert(reef.isWithinBuffer(on, 0.0), "a buffer of zero still holds the reef itself")
    assert(reef.isWithinBuffer(on, 5.0))
  }

  it should "not hold a point beyond the buffer" in {
    val off = new GeoCoordinate(-27.85, 153.5, 5.0)
    assert(!reef.isWithinBuffer(off, 0.0))
    assert(!reef.isWithinBuffer(off, 10.0))
    assert(reef.isWithinBuffer(off, 12.0))
  }

  it should "give the direction from a point towards its centre, in degrees counter-clockwise from east" in {
    // due west is +180 or -180 degrees, depending on which side of zero the rounding falls
    assert(math.abs(math.abs(reef.direction(new GeoCoordinate(-28.0, 153.9, 5.0))) - 180) < 1e-6)
    assert(math.abs(reef.direction(new GeoCoordinate(-28.5, 153.5, 5.0)) - 90) < 1e-6)
    assert(math.abs(reef.direction(new GeoCoordinate(-27.5, 153.5, 5.0)) - (-90)) < 1e-6)
  }
}

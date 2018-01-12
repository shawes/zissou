package maths

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import physical.{GeoCoordinate,Velocity}

class GeometryTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  val geometry = new Geometry()

  "Geometry should move the point" should "...." in {
    val currentLoc = new GeoCoordinate(51.0,0.0)
    val expectedLoc = new GeoCoordinate(51.00089832,0.001427437)
    val velocity = new Velocity(100,100,0)
    val swim = new Velocity(0,0,0)
    val result = geometry.translatePoint(currentLoc,velocity,1,swim, false)
    assert(result.latitude - expectedLoc.latitude < 0.001)
    assert(result.longitude - expectedLoc.longitude < 0.001)
  }

  it should "do the same with implementation2" in {
    val currentLoc = new GeoCoordinate(51.0,0.0)
    val expectedLoc = new GeoCoordinate(51.00089832,0.001427437)
    val velocity = new Velocity(100,100,0)
    val swim = new Velocity(0,0,0)
    val result = geometry.translatePointApproximation(currentLoc,velocity,1,swim, false)
    assert(result.latitude - expectedLoc.latitude < 0.001)
    assert(result.longitude - expectedLoc.longitude < 0.001)
  }

  it should "do the same with implementation3" in {
    val currentLoc = new GeoCoordinate(51.0,0.0)
    val expectedLoc = new GeoCoordinate(51.00089832,0.001427437)
    val velocity = new Velocity(100,100,0)
    val swim = new Velocity(0,0,0)
    val result = geometry.translatePointPecision(currentLoc,velocity,1,swim, false)
    assert(result.latitude - expectedLoc.latitude < 0.001)
    assert(result.longitude - expectedLoc.longitude < 0.001)
  }

  it should "get the distance between to points in metres" in {
    val pointA = new GeoCoordinate(51.0,0.0)
    val pointB = new GeoCoordinate(51.00089832,0.001427437)
    val expected = 141.421
    val result = geometry.getDistanceBetweenTwoPoints(pointA, pointB)
    assert(result - expected < 0.1)
  }

}

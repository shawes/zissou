package physical.flow

import exceptions.CoordinateOutOfBoundsException
import org.mockito.ArgumentMatchers.{any, anyDouble}
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import physical.GeoCoordinate
import ucar.nc2.dt.GridCoordSystem

class FlowGridWrapperBoundsTest extends AnyFlatSpec with MockitoSugar {

  private val depths = List(5.0, 10.0)
  // depth x latitude x longitude, so 4 latitudes and 3 longitudes
  private val slice = Array.fill(2, 4, 3)(0.1f)

  private def wrapperIndexedAt(x: Int, y: Int): FlowGridWrapper = {
    val coordinateSystem = mock[GridCoordSystem]
    when(coordinateSystem.findXYindexFromLatLon(anyDouble(), anyDouble(), any()))
      .thenReturn(Array(x, y))
    new FlowGridWrapper(depths, List(List((slice, coordinateSystem))))
  }

  private val coordinate = new GeoCoordinate(-28.0, 154.0, 5.0)

  "The flow grid wrapper" should "index a coordinate inside the grid" in {
    val index = wrapperIndexedAt(2, 3).getIndex(coordinate)
    assert(index._1 == 2 && index._2 == 3)
  }

  it should "reject a coordinate the index lookup reports as outside the grid" in {
    assertThrows[CoordinateOutOfBoundsException](
      wrapperIndexedAt(-1, -1).getIndex(coordinate)
    )
  }

  it should "reject a coordinate past the last longitude" in {
    assertThrows[CoordinateOutOfBoundsException](
      wrapperIndexedAt(3, 0).getIndex(coordinate)
    )
  }

  it should "reject a coordinate past the last latitude" in {
    assertThrows[CoordinateOutOfBoundsException](
      wrapperIndexedAt(0, 4).getIndex(coordinate)
    )
  }

  it should "reject a coordinate outside the grid when finding its velocity" in {
    assertThrows[CoordinateOutOfBoundsException](
      wrapperIndexedAt(-1, 2).getVelocity(coordinate)
    )
  }
}

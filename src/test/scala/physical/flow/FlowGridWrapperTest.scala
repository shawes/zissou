package physical.flow

import com.github.nscala_time.time.Imports.LocalDateTime
import locals._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid

class FlowGridWrapperTest
    extends AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {
  val depths = List(5.0, 10.0, 15.0, 20.0, 30.0, 40.0, 50.0, 60.0)
  private val earlier = new LocalDateTime(2002, 1, 5, 12, 0)
  private val later = new LocalDateTime(2002, 1, 6, 12, 0)
  private val mockDatasets =
    mock[List[List[(Array[Array[Array[Float]]], GridCoordSystem)]]]

  "The flow grid wrapper" should "intialise" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    assert(flowGridWrapper != null)
  }

  it should "get the correct neighbourhood size for tricubic integration" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    val neighbourhoodSize = PrivateMethod[Int](Symbol("neighbourhoodSize"))
    val result = flowGridWrapper invokePrivate neighbourhoodSize(
      Tricubic
    )
    assert(result == 8)
  }
  it should "get the correct neighbourhood size for bicubic integration" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    val neighbourhoodSize = PrivateMethod[Int](Symbol("neighbourhoodSize"))
    val result = flowGridWrapper invokePrivate neighbourhoodSize(
      Bicubic
    )
    assert(result == 4)
  }

  it should "get the correct neighbourhood size for bilinear integration" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    val neighbourhoodSize = PrivateMethod[Int](Symbol("neighbourhoodSize"))
    val result = flowGridWrapper invokePrivate neighbourhoodSize(
      Bilinear
    )
    assert(result == 2)
  }

  "The weight of the later field" should "be 0 at the time of the earlier field, and before it" in {
    val wrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    assert(wrapper.weightOfLater(earlier) == 0.0)
    assert(wrapper.weightOfLater(earlier.minusHours(5)) == 0.0)
    assert(wrapper.weightOfLater(earlier.minusDays(40)) == 0.0)
  }

  it should "be 1 at the time of the later field, and after it" in {
    val wrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    assert(wrapper.weightOfLater(later) == 1.0)
    assert(wrapper.weightOfLater(later.plusHours(5)) == 1.0)
  }

  it should "grow in proportion to the time between the two fields" in {
    val wrapper = new FlowGridWrapper(depths, mockDatasets, earlier, later)
    assert(wrapper.weightOfLater(earlier.plusHours(6)) == 0.25)
    assert(wrapper.weightOfLater(earlier.plusHours(12)) == 0.5)
    assert(wrapper.weightOfLater(earlier.plusHours(18)) == 0.75)
    assert(math.abs(wrapper.weightOfLater(earlier.plusMinutes(1)) - 1.0 / 1440) < 1e-12)
  }

  it should "follow the times it is given, whatever the gap between the fields" in {
    val threeHourly = new FlowGridWrapper(depths, mockDatasets, earlier, earlier.plusHours(3))
    assert(threeHourly.weightOfLater(earlier.plusMinutes(45)) == 0.25)
    assert(threeHourly.weightOfLater(earlier.plusHours(2)) == 2.0 / 3)
  }

  it should "be 0 when both fields are for the same time" in {
    assert(new FlowGridWrapper(depths, mockDatasets, earlier, earlier).weightOfLater(earlier.plusHours(1)) == 0.0)
  }
}

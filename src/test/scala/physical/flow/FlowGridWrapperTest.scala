package physical.flow

import locals.{InterpolationType, QuadrantType}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.grid.GeoGrid

class FlowGridWrapperTest extends FlatSpec with MockitoSugar with PrivateMethodTester {
  val depths = List(5.0, 10.0, 15.0, 20.0, 30.0, 40.0, 50.0, 60.0)
  private val mockDatasets = mock[List[(Array[Array[Array[Array[Float]]]],GridCoordSystem)]]

  
  "The flow grid wrapper" should "intialise" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    assert(flowGridWrapper != null)
  }
  
  it should "get the correct quadrant position for bicubic integration in the top left" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.TopLeft, 4)
    assert(result ==(-2, -2))
  }
  
  it should "get the correct quadrant position for bicubic integration in the top right" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.TopRight, 4)
    assert(result ==(-1, -2))
  }
  
  it should "get the correct quadrant position for bicubic integration in the bottom right" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.BottomRight, 4)
    assert(result ==(-1, -1))
  }
  
  it should "get the correct quadrant position for bicubic integration in the bottom left" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.BottomLeft, 4)
    assert(result ==(-2, -1))
  }
  
  it should "get the correct quadrant position for bilinear integration in the top left" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.TopLeft, 2)
    assert(result ==(-1, -1))
  }
  
  it should "get the correct quadrant position for bilinear integration in the top right" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.TopRight, 2)
    assert(result ==(0, -1))
  }
  
  it should "get the correct quadrant position for bilinear integration in the bottom right" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.BottomRight, 2)
    assert(result ==(0, 0))
  }
  
  it should "get the correct quadrant position for bilinear integration in the bottom left" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.BottomLeft, 2)
    assert(result ==(-1, 0))
  }
  
  it should "get the correct quadrant position for tricubic integration in the top left" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.TopLeft, 8)
    assert(result ==(-4, -4))
  }
  
  it should "get the correct quadrant position for tricubic integration in the top right" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.TopRight, 8)
    assert(result ==(-3, -4))
  }
  
  it should "get the correct quadrant position for tricubic integration in the bottom right" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.BottomRight, 8)
    assert(result ==(-3, -3))
  }
  
  it should "get the correct quadrant position for tricubic integration in the bottom left" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val quadrantPosition = PrivateMethod[(Int, Int)]('quadrantPosition)
    val result = flowGridWrapper invokePrivate quadrantPosition(QuadrantType.BottomLeft, 8)
    assert(result ==(-4, -3))
  }
  
  it should "get the correct neighbourhood size for tricubic integration" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val neighbourhoodSize = PrivateMethod[Int]('neighbourhoodSize)
    val result = flowGridWrapper invokePrivate neighbourhoodSize(InterpolationType.Tricubic)
    assert(result == 8)
  }
  it should "get the correct neighbourhood size for bicubic integration" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val neighbourhoodSize = PrivateMethod[Int]('neighbourhoodSize)
    val result = flowGridWrapper invokePrivate neighbourhoodSize(InterpolationType.Bicubic)
    assert(result == 4)
  }
  
  it should "get the correct neighbourhood size for bilinear integration" in {
    val flowGridWrapper = new FlowGridWrapper(depths, mockDatasets)
    val neighbourhoodSize = PrivateMethod[Int]('neighbourhoodSize)
    val result = flowGridWrapper invokePrivate neighbourhoodSize(InterpolationType.Bilinear)
    assert(result == 2)
  }
}

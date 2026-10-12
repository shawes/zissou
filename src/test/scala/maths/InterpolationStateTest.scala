package maths

import maths.interpolation.cubic.BicubicInterpolation
import maths.interpolation.linear.BilinearInterpolation
import org.scalatest.flatspec.AnyFlatSpec
import physical.Velocity

class InterpolationStateTest extends AnyFlatSpec {

  private def grid(size: Int, offset: Double): Array[Array[Velocity]] =
    Array.tabulate(size, size)(
      (i, j) => new Velocity(offset + i, offset + 2 * j, offset + i * j)
    )

  "Bicubic interpolation" should "give the same answer however many times it is used" in {
    val interpolation = new BicubicInterpolation()
    val first = interpolation(grid(4, 0.0), 1.3, 1.6)
    interpolation(grid(4, 5.0), 1.1, 1.9)
    val again = interpolation(grid(4, 0.0), 1.3, 1.6)
    assert(again == first)
  }

  "Bilinear interpolation" should "give the same answer however many times it is used" in {
    val interpolation = new BilinearInterpolation()
    val first = interpolation(grid(2, 0.0), 0.3, 0.6)
    interpolation(grid(2, 5.0), 0.1, 0.9)
    val again = interpolation(grid(2, 0.0), 0.3, 0.6)
    assert(again == first)
  }

  it should "interpolate between the values it is given" in {
    val interpolation = new BilinearInterpolation()
    val result = interpolation(
      Array(
        Array(new Velocity(0, 0, 0), new Velocity(1, 1, 1)),
        Array(new Velocity(10, 10, 10), new Velocity(11, 11, 11))
      ),
      0.5,
      0.5
    )
    assert(result.u == 5.5 && result.v == 5.5 && result.w == 5.5)
  }
}

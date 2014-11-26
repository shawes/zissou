package physical

import org.scalatest.FlatSpec

class VelocityTest extends FlatSpec {

  "The constructor" should "default width to zero if not passed as parameter" in {
    val velocity = new Velocity(1, 2)
    assert(velocity.w == 0)
  }

  "A velocity" should "be undefined if u is NaN" in {
    val velocity = new Velocity(Double.NaN, 0, 0)
    assert(velocity.isUndefined)
  }

  it should "be undefined if v is NaN" in {
    val velocity = new Velocity(0, Double.NaN, 0)
    assert(velocity.isUndefined)
  }

  it should "add velocities correctly" in {
    val v1 = new Velocity(1, 2, 3)
    val v2 = new Velocity(4, 5, 6)

    assertResult(5) {
      (v1 + v2).u
    }
    assertResult(7) {
      (v1 + v2).v
    }
    assertResult(9) {
      (v1 + v2).w
    }
  }

  it should "subtract velocities correctly" in {
    val v1 = new Velocity(1, 2, 3)
    val v2 = new Velocity(4, 5, 6)

    assertResult(-3) {
      (v1 - v2).u
    }
    assertResult(-3) {
      (v1 - v2).v
    }
    assertResult(-3) {
      (v1 - v2).w
    }
  }

  it should "multiply velocities correctly" in {
    val v1 = new Velocity(1, 2, 3)
    val v2 = new Velocity(4, 5, 6)

    assertResult(4) {
      (v1 * v2).u
    }
    assertResult(10) {
      (v1 * v2).v
    }
    assertResult(18) {
      (v1 * v2).w
    }
  }

  it should "multiply a velocity by a scalar correctly" in {
    val v1 = new Velocity(1, 2, 3)
    val scalar = 5

    assertResult(5) {
      (v1 * scalar).u
    }
    assertResult(10) {
      (v1 * scalar).v
    }
    assertResult(15) {
      (v1 * scalar).w
    }
  }

  it should "print out the correct style" in {
    val v1 = new Velocity(1, 2, 3)
    assertResult("u=1.0, v=2.0, w=3.0") {
      v1.toString
    }
  }
}

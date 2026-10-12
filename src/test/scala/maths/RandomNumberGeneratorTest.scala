package maths

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.parallel.CollectionConverters._

class RandomNumberGeneratorTest extends AnyFlatSpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = RandomNumberGenerator.setSeed(1234)

  private def draws(key: Long, step: Long): List[Double] =
    RandomNumberGenerator.withStream(key, step) {
      List.fill(5)(RandomNumberGenerator.get)
    }

  "A random stream" should "repeat for the same seed, key and step" in {
    assert(draws(7, 3) == draws(7, 3))
  }

  it should "differ between keys" in {
    assert(draws(7, 3) != draws(8, 3))
  }

  it should "differ between steps" in {
    assert(draws(7, 3) != draws(7, 4))
  }

  it should "differ between seeds" in {
    val first = draws(7, 3)
    RandomNumberGenerator.setSeed(99)
    assert(first != draws(7, 3))
  }

  it should "not consume numbers from the sequential generator" in {
    RandomNumberGenerator.setSeed(5)
    val expected = List.fill(3)(RandomNumberGenerator.get)
    RandomNumberGenerator.setSeed(5)
    draws(1, 1)
    assert(List.fill(3)(RandomNumberGenerator.get) == expected)
  }

  it should "restore the previous stream after a nested stream" in {
    val expected = RandomNumberGenerator.withStream(1, 1) {
      List.fill(4)(RandomNumberGenerator.get)
    }
    val actual = RandomNumberGenerator.withStream(1, 1) {
      val first = RandomNumberGenerator.get
      RandomNumberGenerator.withStream(2, 2)(RandomNumberGenerator.get)
      first :: List.fill(3)(RandomNumberGenerator.get)
    }
    assert(actual == expected)
  }

  it should "give the same numbers whatever order the threads run in" in {
    val keys = (1L to 2000L).toList
    val sequential = keys.map(k => k -> draws(k, 9)).toMap
    val parallel = keys.par.map(k => k -> draws(k, 9)).seq.toMap
    assert(parallel == sequential)
  }

  it should "stay within the requested range" in {
    RandomNumberGenerator.withStream(1, 1) {
      val values = List.fill(1000)(RandomNumberGenerator.get(-3, 4))
      assert(values.forall(v => v >= -3 && v < 4))
      assert(values.exists(_ < 0) && values.exists(_ > 0))
    }
  }

  "The distribution generator" should "follow the current stream" in {
    val sample = () => RandomNumberGenerator.generator.nextDouble()
    val first = RandomNumberGenerator.withStream(3, 3)(List.fill(3)(sample()))
    val second = RandomNumberGenerator.withStream(3, 3)(List.fill(3)(sample()))
    assert(first == second)
  }

  "A key for a string" should "be stable and distinguish ids" in {
    assert(RandomNumberGenerator.keyFor("abc") == RandomNumberGenerator.keyFor("abc"))
    assert(RandomNumberGenerator.keyFor("abc") != RandomNumberGenerator.keyFor("abd"))
  }
}

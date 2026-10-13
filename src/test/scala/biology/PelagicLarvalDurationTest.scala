package biology

import io.config.PelagicLarvalDurationConfig
import maths.RandomNumberGenerator
import org.scalatest.flatspec.AnyFlatSpec

class PelagicLarvalDurationTest extends AnyFlatSpec {

  private def pld(mean: Double, stdev: Double, pldType: String) =
    new PelagicLarvalDuration(PelagicLarvalDurationConfig(mean, stdev, pldType, 1))

  "A fixed pelagic larval duration" should "be the mean, with no standard deviation needed" in {
    assert(pld(15, 0, "fixed").getPld() == 15 * 86400)
    assert(pld(15, 2, "fixed").getPld() == 15 * 86400)
  }

  "A sampled pelagic larval duration" should "be the mean when its standard deviation is 0" in {
    assert(pld(15, 0, "normal").getPld() == 15 * 86400)
  }

  it should "always be longer than nothing, however wide the distribution" in {
    RandomNumberGenerator.setSeed(3)
    val durations = pld(1, 2, "normal")
    val draws = (1 to 10000).map(_ => durations.getPld())
    assert(draws.forall(_ > 0))
    // the draws still spread round the mean
    assert(draws.count(_ < 86400) > 1000 && draws.count(_ > 86400) > 1000)
  }

  "A mean pelagic larval duration" should "be more than 0 days" in {
    intercept[IllegalArgumentException](pld(0, 1, "normal"))
    intercept[IllegalArgumentException](pld(-2, 1, "fixed"))
  }
}

package biology.fish

import java.io.File
import com.github.nscala_time.time.Imports._
import biology.SpawningLocation
import org.scalatest.flatspec.AnyFlatSpec
import physical.GeoCoordinate
import tools.ModelFixture

class FishFactoryTest extends AnyFlatSpec {

  private def larvaConfig(larva: String) =
    ModelFixture.configuration(ModelFixture.yaml(new File("unused"), larva)).larva

  private val site = {
    val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
    new SpawningLocation("reef", 10, 101, new GeoCoordinate(-28.0, 153.5, 5.0), start to start.plusDays(1), 1)
  }

  private def larvaWith(pld: Double, competency: Double) =
    new FishFactory(larvaConfig(ModelFixture.driftingLarva(pld = pld, nonSettlementPeriod = competency)))
      .create(site, new LocalDateTime(2002, 1, 5, 0, 0))

  "A larva's competency period" should "be the one configured, in seconds" in {
    val larva = larvaWith(pld = 15, competency = 5)
    assert(larva.nonSettlementPeriod == 5 * 86400)
  }

  it should "be no longer than its pelagic larval duration" in {
    val larva = larvaWith(pld = 15, competency = 20)
    assert(larva.nonSettlementPeriod == larva.pelagicLarvalDuration)
  }

  "A larva's stages of development" should "begin in order, and not before it is born, when their ages are close" in {
    maths.RandomNumberGenerator.setSeed(5)
    // each age is drawn with a standard deviation of half a day
    val factory = new FishFactory(larvaConfig(ModelFixture.driftingLarva(pld = 15, ontogeny = (1, 1, 1))))
    val fish = (1 to 2000).map(_ => factory.create(site, new LocalDateTime(2002, 1, 5, 0, 0)).asInstanceOf[Fish])
    assert(fish.forall(f => 0 <= f.preflexion && f.preflexion <= f.flexion && f.flexion <= f.postflexion))
    // still drawn, not all the same
    assert(fish.map(_.postflexion).distinct.size > 1000)
  }

  it should "not need a hatching age, which is not used, but accept one" in {
    val withHatching = ModelFixture.driftingLarva(pld = 15)
    val withoutHatching = withHatching.replace("    hatching: 0\n", "")
    assert(withoutHatching != withHatching)
    for (larva <- Seq(withHatching, withoutHatching)) {
      val fish = new FishFactory(larvaConfig(larva)).create(site, new LocalDateTime(2002, 1, 5, 0, 0))
      assert(fish.pelagicLarvalDuration == 15 * 86400)
    }
  }

  it should "begin at preflexion when there is no egg stage" in {
    val factory = new FishFactory(larvaConfig(ModelFixture.driftingLarva(pld = 15, ontogeny = (0, 1, 2))))
    assert((1 to 100).forall(_ => factory.create(site, new LocalDateTime(2002, 1, 5, 0, 0)).asInstanceOf[Fish].preflexion == 0))
  }
}

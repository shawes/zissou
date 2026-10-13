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
}

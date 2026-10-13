package biology.swimming

import org.scalatest.flatspec.AnyFlatSpec
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec

class VerticalMigrationDielTest
    extends AnyFlatSpec
    with MockitoSugar
    with PrivateMethodTester {

  "A diel vertical migration" should "initialise" in {
    val dielMigration =
      new DielMigration(
        List.empty[Double],
        List.empty[Double],
        List.empty[Double]
      )
    assert(dielMigration != null)
  }

  it should "choose a depth from the day probabilities by day and the night ones by night" in {
    // always deep by day, always shallow by night
    val dielMigration = new DielMigration(List(25, 50, 75, 100), List(0, 0, 0, 1), List(1, 0, 0, 0))
    for (_ <- 1 to 100) {
      val day = dielMigration(locals.Day, 50.0)
      val night = dielMigration(locals.Night, 50.0)
      assert(day >= 76 && day <= 100, s"$day m by day")
      assert(night >= 0 && night <= 25, s"$night m by night")
    }
  }

  it should "leave a larva where it is at dawn or dusk when the probabilities for it are all 0" in {
    val stillByDay = new DielMigration(List(25, 50, 75, 100), List(0, 0, 0, 0), List(1, 0, 0, 0))
    val stillByNight = new DielMigration(List(25, 50, 75, 100), List(0, 0, 0, 1), List(0, 0, 0, 0))
    for (depth <- Seq(2.5, 40.0, 90.0)) {
      assert(stillByDay(locals.Day, depth) == depth)
      assert(stillByNight(locals.Night, depth) == depth)
    }
    assert(!stillByDay.moves(locals.Day) && stillByDay.moves(locals.Night))
    assert(stillByNight.moves(locals.Day) && !stillByNight.moves(locals.Night))
    assert(stillByDay(locals.Night, 90.0) <= 25 && stillByNight(locals.Day, 2.5) >= 76)
  }

}

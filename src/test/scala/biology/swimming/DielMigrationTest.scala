package biology.swimming

import org.scalatest._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

class VerticalMigrationDielTest
    extends flatspec.AnyFlatSpec
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

}

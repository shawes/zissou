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

}

package biology.swimming

import org.scalatest.FlatSpec
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class VerticalMigrationDielTest
    extends FlatSpec
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

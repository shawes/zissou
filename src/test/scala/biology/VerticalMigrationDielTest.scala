package biology

import org.scalatest.FlatSpec
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, PrivateMethodTester}

class VerticalMigrationDielTest extends FlatSpec with MockitoSugar with PrivateMethodTester {

  "A diel vertical migration" should "initialise" in {
    val dielMigration = new VerticalMigrationDiel(List.empty[VerticalMigrationDielProbability])
    assert(dielMigration != null)
  }

}
